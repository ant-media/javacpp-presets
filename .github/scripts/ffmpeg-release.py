#!/usr/bin/env python3
"""Prepare, stage, and publish this fork's Linux/macOS FFmpeg release pack."""
import argparse
import base64
import os
from pathlib import Path
import re
import shutil
import subprocess
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

NS = 'http://maven.apache.org/POM/4.0.0'
ET.register_namespace('', NS)
N = {'m': NS}
ROOT = Path(__file__).resolve().parents[2]
PLATFORMS = ('linux-arm64', 'linux-x86_64', 'macosx-arm64', 'macosx-x86_64')
CLASSIFIERS = tuple(p + ext for p in PLATFORMS for ext in ('', '-gpl'))
REPOSITORY = 'https://maven.pkg.github.com/ant-media/javacpp-presets'
DEPLOY = 'org.apache.maven.plugins:maven-deploy-plugin:3.1.4'
BUNDLE = ROOT / 'release-bundle'


def element(parent, tag, text=None):
    node = ET.SubElement(parent, '{' + NS + '}' + tag)
    node.text = text
    return node


def prepare(version):
    root_version = ET.parse(ROOT / 'pom.xml').findtext('m:version', namespaces=N)
    if root_version != '1.5.13':
        raise ValueError('Recheck JavaCPP compatibility before changing the 1.5.13 release baseline')
    if not re.fullmatch(r'8\.1-1\.5\.13-antmedia[1-9][0-9]*', version):
        raise ValueError('Expected 8.1-1.5.13-antmediaN, for example 8.1-1.5.13-antmedia1')
    for relative in ('ffmpeg/pom.xml', 'ffmpeg/platform/pom.xml', 'ffmpeg/platform/gpl/pom.xml'):
        path = ROOT / relative
        tree = ET.parse(path)
        project = tree.getroot()
        project.find('m:parent/m:version', N).text = root_version
        project.find('m:version', N).text = version
        # These aggregators must only depend on classifiers in the release pack.
        dependencies = project.find('m:dependencies', N)
        for dependency in list(dependencies):
            classifier = dependency.findtext('m:classifier', namespaces=N) or ''
            if any(excluded in classifier for excluded in ('android', 'windows')):
                dependencies.remove(dependency)
        for entry in project.iter():
            if entry.tag.endswith('Class-Path') and entry.text:
                entry.text = ' '.join(part for part in entry.text.split()
                                      if not any(excluded in part for excluded in ('android', 'windows')))
            if entry.tag.endswith('moduleInfoSource') and entry.text:
                entry.text = '\n'.join(line for line in entry.text.splitlines()
                                       if not any(excluded in line for excluded in ('.android.', '.windows.')))
        repositories = project.find('m:repositories', N)
        if repositories is None:
            repositories = element(project, 'repositories')
        if not any(repo.findtext('m:id', namespaces=N) == 'github' for repo in repositories):
            repo = element(repositories, 'repository')
            element(repo, 'id', 'github')
            element(repo, 'url', REPOSITORY)
        tree.write(path, encoding='UTF-8', xml_declaration=True)
    print('Prepared FFmpeg ' + version + ' with JavaCPP ' + root_version)


def collect(platform, extension):
    classifier = platform + extension
    if classifier not in CLASSIFIERS:
        raise ValueError('Unexpected classifier: ' + classifier)
    native = ROOT / 'ffmpeg/target' / ('ffmpeg-' + classifier + '.jar')
    with zipfile.ZipFile(native) as archive:
        prefix = 'org/bytedeco/ffmpeg/' + classifier + '/'
        if not any(name.startswith(prefix) and 'jniavutil' in name for name in archive.namelist()):
            raise ValueError('Missing FFmpeg JNI library in ' + str(native))
    target = BUNDLE / classifier
    target.mkdir(parents=True, exist_ok=True)
    shutil.copy2(native, target / native.name)
    if classifier == 'linux-x86_64':
        common = BUNDLE / 'common'
        common.mkdir(parents=True, exist_ok=True)
        for name in ('ffmpeg.jar', 'ffmpeg-sources.jar', 'ffmpeg-javadoc.jar'):
            shutil.copy2(ROOT / 'ffmpeg/target' / name, common / name)
        shutil.copy2(ROOT / 'ffmpeg/pom.xml', common / 'ffmpeg.pom')
    print('Staged ' + classifier)


def registry_exists(relative):
    credentials = os.environ['GITHUB_ACTOR'] + ':' + os.environ['GITHUB_TOKEN']
    request = urllib.request.Request(REPOSITORY + '/' + relative)
    request.add_header('Authorization', 'Basic ' + base64.b64encode(credentials.encode()).decode())
    try:
        with urllib.request.urlopen(request, timeout=60):
            return True
    except urllib.error.HTTPError as error:
        if error.code == 404:
            return False
        raise


def maven(*arguments):
    subprocess.run(['mvn', '-B', '--no-transfer-progress', *arguments], cwd=ROOT, check=True)


def publish(version):
    prepare(version)
    # Validate the complete staged pack before publishing any Maven artifacts.
    files = [BUNDLE / classifier / ('ffmpeg-' + classifier + '.jar') for classifier in CLASSIFIERS]
    common = BUNDLE / 'common'
    for path in files + [common / name for name in
                         ('ffmpeg.jar', 'ffmpeg-sources.jar', 'ffmpeg-javadoc.jar', 'ffmpeg.pom')]:
        if not path.is_file():
            raise ValueError('Incomplete release pack: missing ' + str(path))
    for classifier, path in zip(CLASSIFIERS, files):
        with zipfile.ZipFile(path) as archive:
            prefix = 'org/bytedeco/ffmpeg/' + classifier + '/'
            if not any(name.startswith(prefix) and 'jniavutil' in name for name in archive.namelist()):
                raise ValueError('Invalid native classifier: ' + classifier)
    pom = ET.parse(common / 'ffmpeg.pom')
    if pom.findtext('m:version', namespaces=N) != version:
        raise ValueError('Staged POM version differs from requested release')
    if registry_exists('org/bytedeco/ffmpeg/' + version + '/ffmpeg-' + version + '.pom'):
        raise ValueError('FFmpeg ' + version + ' already exists; use a new antmedia revision')
    parent = 'org/bytedeco/javacpp-presets/1.5.13/javacpp-presets-1.5.13.pom'
    if not registry_exists(parent):
        maven('-N', 'package', DEPLOY + ':deploy', '-DaltDeploymentRepository=github::' + REPOSITORY)
    # One upload invocation owns the shared Java artifacts and all classifiers.
    maven('-N', DEPLOY + ':deploy-file', '-DrepositoryId=github', '-Durl=' + REPOSITORY,
          '-DpomFile=' + str(common / 'ffmpeg.pom'), '-Dfile=' + str(common / 'ffmpeg.jar'),
          '-DgroupId=org.bytedeco', '-DartifactId=ffmpeg', '-Dversion=' + version,
          '-Dfiles=' + ','.join(str(path) for path in files),
          '-Dclassifiers=' + ','.join(CLASSIFIERS), '-Dtypes=' + ','.join('jar' for _ in files),
          '-Dsources=' + str(common / 'ffmpeg-sources.jar'),
          '-Djavadoc=' + str(common / 'ffmpeg-javadoc.jar'))
    for pom_path in ('ffmpeg/platform/pom.xml', 'ffmpeg/platform/gpl/pom.xml'):
        maven('-f', pom_path, 'package', DEPLOY + ':deploy', '-Djavacpp.platform.host',
              '-DaltDeploymentRepository=github::' + REPOSITORY)
    summary = os.environ.get('GITHUB_STEP_SUMMARY')
    if summary:
        with open(summary, 'a') as output:
            output.write('Published `org.bytedeco:ffmpeg:' + version + '` and both platform aggregators.\n\n')
            output.write('Classifiers: ' + ', '.join('`' + value + '`' for value in CLASSIFIERS) + '\n')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest='command', required=True)
    commands.add_parser('prepare').add_argument('version')
    collector = commands.add_parser('collect')
    collector.add_argument('platform', choices=PLATFORMS)
    collector.add_argument('extension', nargs='?', default='', choices=('', '-gpl'))
    commands.add_parser('publish')
    args = parser.parse_args()
    if args.command == 'prepare':
        prepare(args.version)
    elif args.command == 'collect':
        collect(args.platform, args.extension)
    else:
        publish(os.environ['ANTMEDIA_FFMPEG_RELEASE_VERSION'])


if __name__ == '__main__':
    main()
