import io
import shutil
import stat
import subprocess
import tempfile
import zipfile
from pathlib import Path


BASELINE_COMMIT = "5b1a3e095f2f1e5033ed2416b8929f8650c31581"


def unpack_baseline(content, directory):
    with zipfile.ZipFile(io.BytesIO(content)) as archive:
        for entry in archive.infolist():
            target = (directory / entry.filename).resolve()
            if not target.is_relative_to(directory.resolve()):
                raise ValueError("Baseline archive contains an unsafe path")
            name = target.name.lower()
            if ((name.startswith(".env") and not name.endswith(".example"))
                    or target.suffix.lower() in {".p12", ".pfx", ".pem", ".key", ".jks"}
                    or stat.S_ISLNK(entry.external_attr >> 16)):
                raise ValueError("Baseline archive contains a credential path or symbolic link")
        archive.extractall(directory)


def main():
    root = Path(__file__).resolve().parent.parent
    maven = shutil.which("mvn") or shutil.which("mvn.cmd")
    if maven is None:
        raise SystemExit("Maven is required")
    commit = subprocess.check_output(
        ["git", "rev-parse", "v1.2.3^{commit}"], cwd=root, text=True).strip()
    if commit != BASELINE_COMMIT:
        raise SystemExit("The v1.2.3 tag does not match the reviewed baseline commit")
    content = subprocess.check_output(["git", "archive", "--format=zip", commit], cwd=root)
    with tempfile.TemporaryDirectory(prefix="arca-compatibility-") as temporary:
        baseline = Path(temporary) / "baseline"
        baseline.mkdir()
        unpack_baseline(content, baseline)
        print(f"Building compatibility baseline {commit}", flush=True)
        subprocess.run([maven, "package", "-DskipTests", "-Djapicmp.skip=true",
                        "--batch-mode", "--no-transfer-progress"], cwd=baseline, check=True)
        subprocess.run([maven, "clean", "verify", "-Pci,compatibility",
                        f"-DcompatibilityBaselineDirectory={baseline}",
                        "--batch-mode", "--no-transfer-progress"], cwd=root, check=True)


if __name__ == "__main__":
    main()
