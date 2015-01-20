import sys
import unittest

from subprocess import Popen, PIPE, STDOUT
from uiautomator import device as d
from state_machine_crawler import State, Transition, StateMachineCrawler


class CommandExecutionError(Exception):
    pass


def call(*command):
    process = Popen(command, shell=True, stdout=PIPE, stderr=STDOUT)

    outputs = []

    # Poll process for new output until finished
    while True:
        nextline = process.stdout.readline()
        if nextline == '' and process.poll() is not None:
            break
        sys.stdout.write(nextline)
        outputs.append(nextline)
        sys.stdout.flush()

    output = "\n".join(outputs)
    exitCode = process.returncode

    if exitCode == 0:
        return output
    else:
        raise CommandExecutionError(command, exitCode, output)


class InstalledState(State):

    def verify(self):
        cmd = "adb shell 'pm list packages -f | grep com.fsecure.lokki'"
        return call(cmd)


class InitialTransition(Transition):
    target_state = InstalledState

    def move(self):
        cmd = "adb install -r App/build/outputs/apk/lokki-*-debug.apk"
        call(cmd)


class LokkiLaunchedState(State):

    def verify(self):
        return d(packageName='com.fsecure.lokki').wait.exists(timeout=10000)

    class LaunchLokkiTransition(Transition):
        source_state = InstalledState

        def move(self):
            cmd = "adb shell am start -n com.fsecure.lokki/com.fsecure.lokki.MainActivity"
            call(cmd)


class BaseTest(unittest.TestCase):

    def setUp(self):
        self.cr = StateMachineCrawler(d, InitialTransition)

    def test_lokki_is_installed(self):
        self.cr.move(InstalledState)
        self.assertIs(self.cr.state, InstalledState)

    def test_lokki_is_launched(self):
        self.cr.move(LokkiLaunchedState)
        self.assertIs(self.cr.state, LokkiLaunchedState)
