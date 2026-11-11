#!/usr/bin/env python3
"""Provision FFmpeg's pinned CUDA toolkit and package its Linux NPP dependencies."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import platform as host_platform
import re
import shutil
import subprocess
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[2]
LOCK = json.loads(Path(__file__).with_name('ffmpeg-cuda-lock.json').read_text())
BASE_URL = 'https://developer.download.nvidia.com/compute/cuda/redist/'
MACHINES = {'linux-x86_64': 'Advanced Micro Devices X86-64', 'linux-arm64': 'AArch64'}
# Keep this list aligned with avfilter.java so JavaCPP extracts these before loading avfilter.
PRELOADS = ('libcudart.so.12', 'libnppc.so.12', 'libnppidei.so.12',
            'libnppicc.so.12', 'libnppig.so.12', 'libnppif.so.12')
SYSTEM = re.compile(r'^(lib(c|m|pthread|dl|rt|gcc_s|stdc\+\+)\.so(?:\.|$)|ld-linux)')
DRIVER = re.compile(r'^lib(cuda|nvcuvid|nvidia-[^.]+)\.so(?:\.|$)')
CUDA = re.compile(r'^lib(npp|cudart|nvrtc|nvJitLink|nvfatbin|cublas)')


def run(*args):
    return subprocess.check_output(args, text=True, env={**os.environ, 'LC_ALL': 'C'})


def sha256(path):
    with open(path, 'rb') as stream:
        digest = hashlib.sha256()
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(chunk)
    return digest.hexdigest()


def elf(path, target):
    header = run('readelf', '-h', str(path))
    machine = re.search(r'Machine:\s*(.*)', header).group(1).strip()
    if machine != MACHINES[target]:
        raise ValueError(f'{path}: expected {MACHINES[target]}, found {machine}')
    dynamic = run('readelf', '-d', str(path))
    needed = re.findall(r'\(NEEDED\).*\[(.*?)\]', dynamic)
    soname = re.findall(r'\(SONAME\).*\[(.*?)\]', dynamic)
    return needed, soname[0] if soname else path.name


def install(target, prefix, cache):
    if host_platform.machine() != 'x86_64':
        raise ValueError('The release toolkit installer expects an x86_64 build host')
    target_arch = 'linux-x86_64' if target == 'linux-x86_64' else 'linux-sbsa'
    prefix.mkdir(parents=True, exist_ok=True)
    cache.mkdir(parents=True, exist_ok=True)
    for component, architectures in LOCK['packages'].items():
        arch = 'linux-x86_64' if component == 'cuda_nvcc' else target_arch
        package = architectures[arch]
        archive = cache / Path(package['relative_path']).name
        if not archive.exists() or sha256(archive) != package['sha256']:
            download = archive.with_suffix('.download-' + str(os.getpid()))
            subprocess.run(['curl', '--fail', '--location', '--retry', '3',
                            BASE_URL + package['relative_path'], '-o', str(download)], check=True)
            if sha256(download) != package['sha256']:
                raise ValueError('SHA-256 mismatch for ' + download.name)
            download.replace(archive)
        subprocess.run(['tar', '-xJf', str(archive), '--strip-components=1', '-C', str(prefix)], check=True)
        notices = prefix / 'licenses' / component
        notices.mkdir(parents=True, exist_ok=True)
        for license_file in prefix.glob('*LICENSE*'):
            if license_file.is_file():
                shutil.copy2(license_file, notices / license_file.name)
    # Redistributable archives use lib; the presets also search the conventional lib64 path.
    if not (prefix / 'lib64').exists():
        (prefix / 'lib64').symlink_to('lib')
    compiler = 'gcc' if target == 'linux-x86_64' else 'aarch64-linux-gnu-gcc'
    compiler_path = shutil.which(compiler)
    if not compiler_path:
        raise ValueError('Missing target compiler: ' + compiler)
    run(str(prefix / 'bin/nvcc'), '--version')  # Always a host executable, including ARM64 builds.
    for name in PRELOADS:
        elf(prefix / 'lib' / name, target)
    # FFmpeg compiles CUDA kernels to PTX, then embeds them in target-architecture objects.
    # Check that the host nvcc and target preprocessor work together without running GPU code.
    with tempfile.TemporaryDirectory() as temporary:
        source = Path(temporary) / 'kernel.cu'
        source.write_text('__global__ void kernel() {}\n')
        subprocess.run([str(prefix / 'bin/nvcc'), '-ccbin', compiler_path, '-ptx',
                        '-arch=compute_75', str(source), '-o', str(source.with_suffix('.ptx'))], check=True)
    values = {'CUDA_PATH': str(prefix), 'CUDA_LIBDIR': str(prefix / 'lib'),
              'NVCC_CCBIN': compiler_path}
    env_file = os.environ.get('GITHUB_ENV')
    if env_file:
        with open(env_file, 'a') as output:
            output.writelines(key + '=' + value + '\n' for key, value in values.items())
    path_file = os.environ.get('GITHUB_PATH')
    if path_file:
        with open(path_file, 'a') as output:
            output.write(str(prefix / 'bin') + '\n')
    print(json.dumps(values, indent=2))


def bundle(target):
    libraries = Path(os.environ['CUDA_LIBDIR']).resolve()
    native = ROOT / 'ffmpeg/target/native/org/bytedeco/ffmpeg' / target
    jar = ROOT / 'ffmpeg/target' / ('ffmpeg-' + target + '.jar')
    config = ROOT / 'ffmpeg/cppbuild' / target / 'ffmpeg-8.1/ffbuild/config.mak'
    if 'CONFIG_SCALE_NPP_FILTER=yes' not in config.read_text():
        raise ValueError('The built FFmpeg does not enable scale_npp')
    queue = list(native.glob('*.so*'))
    for name in PRELOADS:
        source = libraries / name
        if 'stubs' in source.resolve().parts:
            raise ValueError('Cannot package a CUDA stub: ' + str(source))
        elf(source, target)
        shutil.copy2(source, native / name)
        queue.append(native / name)
    visited = set()
    while queue:
        path = queue.pop()
        if path.name in visited:
            continue
        visited.add(path.name)
        if DRIVER.match(path.name):
            raise ValueError('Driver library must not be packaged: ' + path.name)
        needed, _ = elf(path, target)
        for dependency in needed:
            if DRIVER.match(dependency) or SYSTEM.match(dependency):
                continue
            bundled = native / dependency
            if bundled.exists():
                queue.append(bundled)
            elif (libraries / dependency).is_file():
                source = libraries / dependency
                if 'stubs' in source.resolve().parts:
                    raise ValueError('Cannot package a CUDA stub: ' + str(source))
                shutil.copy2(source, bundled)
                queue.append(bundled)
            elif CUDA.match(dependency) or path.name in PRELOADS:
                raise ValueError('Unresolved CUDA/NPP dependency: ' + dependency)
        subprocess.run(['patchelf', '--set-rpath', '$ORIGIN', str(path)], check=True)
    entries = {}
    for path in sorted(native.glob('*.so*')):
        needed, soname = elf(path, target)
        entries[path.name] = {'soname': soname, 'needed': needed, 'sha256': sha256(path)}
    # A dependency inside a jar is only usable if JavaCPP extracts it. Its preloads
    # must cover the CUDA dependency closure; do not silently add uncached libraries.
    for name, info in entries.items():
        for dependency in info['needed']:
            if CUDA.match(dependency) and dependency not in PRELOADS:
                raise ValueError('Add CUDA dependency to avfilter preloads before publishing: ' + dependency)
    report = {'cuda_version': LOCK['cuda_version'], 'platform': target, 'libraries': entries}
    (native / 'cuda-runtime-manifest.json').write_text(json.dumps(report, indent=2) + '\n')
    notices = native / 'cuda-licenses'
    notices.mkdir(exist_ok=True)
    shutil.copytree(Path(os.environ['CUDA_PATH']) / 'licenses', notices, dirs_exist_ok=True)
    resource = 'org/bytedeco/ffmpeg/' + target + '/'
    replacements = {resource + path.relative_to(native).as_posix(): path
                    for path in native.rglob('*') if path.is_file()}
    staged = jar.with_suffix('.staged.jar')
    with zipfile.ZipFile(jar) as original, zipfile.ZipFile(staged, 'w', zipfile.ZIP_DEFLATED) as output:
        for info in original.infolist():
            if info.filename not in replacements:
                output.writestr(info, original.read(info.filename))
        for name, path in replacements.items():
            output.write(path, name)
    staged.replace(jar)
    inspect(target, jar)


def inspect(target, jar):
    resource = 'org/bytedeco/ffmpeg/' + target + '/'
    with zipfile.ZipFile(jar) as archive, tempfile.TemporaryDirectory() as temporary:
        report = json.loads(archive.read(resource + 'cuda-runtime-manifest.json'))
        if report['platform'] != target or report['cuda_version'] != LOCK['cuda_version']:
            raise ValueError('CUDA artifact manifest does not match this build')
        for name in PRELOADS:
            if name not in report['libraries']:
                raise ValueError('Missing JavaCPP preload in native jar: ' + name)
        for name in archive.namelist():
            if name.startswith(resource) and ('stubs' in Path(name).parts or DRIVER.match(Path(name).name)):
                raise ValueError('Native jar contains a driver/stub library: ' + name)
        for name, info in report['libraries'].items():
            path = Path(temporary) / name
            path.write_bytes(archive.read(resource + name))
            if sha256(path) != info['sha256']:
                raise ValueError('Library checksum mismatch: ' + name)
            needed, soname = elf(path, target)
            if needed != info['needed'] or soname != info['soname']:
                raise ValueError('ELF metadata mismatch: ' + name)
            if run('patchelf', '--print-rpath', str(path)).strip() != '$ORIGIN':
                raise ValueError('Missing bundled library search path: ' + name)
            for dependency in needed:
                if CUDA.match(dependency) and dependency not in report['libraries']:
                    raise ValueError('Missing CUDA/NPP dependency in native jar: ' + dependency)
    print('Verified NPP runtime closure and ELF architecture in ' + str(jar))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest='command', required=True)
    provision = commands.add_parser('install')
    provision.add_argument('platform', choices=MACHINES)
    provision.add_argument('--prefix', type=Path, required=True)
    provision.add_argument('--cache', type=Path, required=True)
    commands.add_parser('bundle').add_argument('platform', choices=MACHINES)
    check = commands.add_parser('inspect')
    check.add_argument('platform', choices=MACHINES)
    check.add_argument('jar', type=Path)
    args = parser.parse_args()
    if args.command == 'install':
        install(args.platform, args.prefix.resolve(), args.cache.resolve())
    elif args.command == 'bundle':
        bundle(args.platform)
    else:
        inspect(args.platform, args.jar)


if __name__ == '__main__':
    main()
