from os import walk
import subprocess
import os


def exec_cmd(cmd):
    process = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    stdout, stderr = process.communicate()
    print(stdout.decode("utf-8"), end="")

    process.wait()
    if process.returncode != 0:
        exit()


filenames = next(walk("./data"), (None, None, []))[2]  # [] if no file
filenames.sort(key = lambda name : int(name.split("_")[1]))

for name in filenames:
    print("try compile")
    exec_cmd("javac *.java")
    print("compile success")
    exec_cmd("python solver.py ./data/{}".format(name))
