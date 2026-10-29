#!/usr/bin/env python3
"""Exercise rebuild control flow without downloading or compiling dependencies."""
import os
from pathlib import Path
import re
import subprocess
import tempfile
import unittest

RECIPE = Path(__file__).resolve().parents[1] / 'cppbuild.sh'
VERSION = re.search(r'^FFMPEG_VERSION=(.*)$', RECIPE.read_text(), re.M)[1]


class BuildModes(unittest.TestCase):
    def run_recipe(self, platform='linux-x86_64', npp=False, prepared=True,
                   configure_ok=True, extension='', mfx=False):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            prefix = root / (platform + extension)
            source = prefix / ('ffmpeg-' + VERSION)
            source.mkdir(parents=True)
            if prepared:
                (source / 'ffbuild').mkdir()
                (source / 'ffbuild/config.mak').touch()
                (prefix / 'include').mkdir()
                (prefix / 'lib').mkdir()
            if mfx:
                (prefix / 'lib/pkgconfig').mkdir()
                (prefix / 'lib/pkgconfig/libmfx.pc').touch()
            configure = source / 'configure'
            configure.write_text('#!/bin/bash\nprintf "%s\\n" "$@" > "$TEST_ARGS"\n'
                                 'printf "%s\\n" "${CPATH:-}" "${LIBRARY_PATH:-}" > "$TEST_CUDA"\n'
                                 + ('exit 0\n' if configure_ok else 'exit 42\n'))
            configure.chmod(0o755)
            cuda = root / 'cuda'
            for folder in ['include', 'bin', 'lib64']:
                (cuda / folder).mkdir(parents=True)
            (cuda / 'include/npp.h').touch()
            (cuda / 'bin/nvcc').touch()
            (cuda / 'bin/nvcc').chmod(0o755)
            environment = dict(os.environ, PLATFORM=platform, EXTENSION=extension,
                               FFMPEG_REBUILD='1', FFMPEG_NPP=str(int(npp)),
                               MAKEJ='2', USERLAND_PATH=str(root), CUDA_PATH=str(cuda),
                               TEST_ARGS=str(root / 'args'), TEST_CUDA=str(root / 'cuda-env'),
                               TEST_MAKE=str(root / 'make'))
            shell = '''set -eu
                download() { echo UNEXPECTED_DOWNLOAD; exit 90; }
                tar() { echo UNEXPECTED_TAR; exit 91; }
                patch() { echo UNEXPECTED_PATCH; exit 92; }
                make() { printf '%s\\n' "$*" >> "$TEST_MAKE"; }
                source "$1"
            '''
            result = subprocess.run(['bash', '-c', shell, 'test', str(RECIPE)],
                                    cwd=root, env=environment, text=True,
                                    stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
            def read(name):
                path = root / name
                return path.read_text() if path.exists() else ''
            return result, read('args'), read('make'), read('cuda-env')

    def test_rebuild_platforms(self):
        for platform in ['linux-x86_64', 'linux-arm64']:
            with self.subTest(platform=platform):
                result, args, make, _ = self.run_recipe(platform)
                self.assertEqual(result.returncode, 0, result.stdout)
                self.assertIn('--enable-libsrt', args)
                self.assertNotIn('--enable-libnpp', args)
                self.assertEqual(make.splitlines(), ['-j 2', 'install'])

    def test_npp_and_gpl_flags(self):
        for platform in ['linux-x86_64', 'linux-arm64']:
            with self.subTest(platform=platform):
                result, args, _, cuda = self.run_recipe(platform, npp=True, extension='-gpl')
                self.assertEqual(result.returncode, 0, result.stdout)
                for flag in ['--enable-cuda-nvcc', '--enable-libnpp', '--enable-nonfree', '--enable-gpl']:
                    self.assertIn(flag, args)
                self.assertIn('/cuda/include', cuda)
                self.assertIn('/cuda/lib64', cuda)

    def test_rebuild_keeps_mfx(self):
        result, args, _, _ = self.run_recipe(mfx=True)
        self.assertEqual(result.returncode, 0, result.stdout)
        self.assertIn('--enable-libmfx', args)
        self.assertIn('-lva-drm', args)

    def test_missing_full_build_fails(self):
        result, _, make, _ = self.run_recipe(prepared=False)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('requires a completed FFmpeg', result.stdout)
        self.assertEqual(make, '')

    def test_configure_failure_stops_build(self):
        result, _, make, _ = self.run_recipe(configure_ok=False)
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(make, '')

    def test_unsupported_platform_fails_before_download(self):
        result, _, make, _ = self.run_recipe('macosx-x86_64')
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('supports linux-x86_64 and linux-arm64 only', result.stdout)
        self.assertNotIn('UNEXPECTED', result.stdout)
        self.assertEqual(make, '')


if __name__ == '__main__':
    unittest.main()
