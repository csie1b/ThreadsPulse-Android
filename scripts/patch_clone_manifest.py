import sys
import os
import re
import zipfile
import io

def patch_manifest_bytes(data: bytes, old_pkg="com.instagram.barcelona", new_pkg="com.instagram.barcelonp") -> bytes:
    assert len(old_pkg) == len(new_pkg), f"Lengths must match: {len(old_pkg)} != {len(new_pkg)}"
    
    # 1. UTF-8 replacement
    old_u8 = old_pkg.encode("utf-8")
    new_u8 = new_pkg.encode("utf-8")
    count_u8 = data.count(old_u8)
    data = data.replace(old_u8, new_u8)
    
    # 2. UTF-16LE replacement
    old_u16 = old_pkg.encode("utf-16le")
    new_u16 = new_pkg.encode("utf-16le")
    count_u16 = data.count(old_u16)
    data = data.replace(old_u16, new_u16)
    
    print(f"[+] Replaced {count_u8} UTF-8 occurrences and {count_u16} UTF-16LE occurrences in binary manifest")
    return data

def patch_apk(input_apk: str, output_apk: str, new_pkg="com.instagram.barcelonp"):
    print(f"[*] Patching APK: {input_apk} -> {output_apk}")
    with zipfile.ZipFile(input_apk, "r") as zin, zipfile.ZipFile(output_apk, "w") as zout:
        for item in zin.infolist():
            data = zin.read(item.filename)
            if item.filename == "AndroidManifest.xml":
                data = patch_manifest_bytes(data, new_pkg=new_pkg)
            zout.writestr(item, data)
    print(f"[✔] Successfully created cloned base APK: {output_apk}")

def patch_decompiled_dir(clone_dir: str):
    manifest_path = os.path.join(clone_dir, "AndroidManifest.xml")
    if not os.path.exists(manifest_path):
        print(f"Error: {manifest_path} not found")
        sys.exit(1)

    with open(manifest_path, "r", encoding="utf-8") as f:
        content = f.read()

    content = content.replace('package="com.instagram.barcelona"', 'package="com.instagram.barcelona.pulse"')
    content = re.sub(
        r'android:authorities="com\.instagram\.barcelona([^"]*)"',
        r'android:authorities="com.instagram.barcelona.pulse\1"',
        content
    )

    with open(manifest_path, "w", encoding="utf-8") as f:
        f.write(content)

    print("[+] Patched plain text AndroidManifest.xml package and authorities")

    yml_path = os.path.join(clone_dir, "apktool.yml")
    if os.path.exists(yml_path):
        with open(yml_path, "r", encoding="utf-8") as f:
            yml = f.read()
        if "renameManifestPackage:" not in yml:
            yml += "\nrenameManifestPackage: com.instagram.barcelona.pulse\n"
            with open(yml_path, "w", encoding="utf-8") as f:
                f.write(yml)
            print("[+] Updated apktool.yml renameManifestPackage")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage:")
        print("  python patch_clone_manifest.py <input.apk> <output.apk>")
        print("  python patch_clone_manifest.py <decompiled_dir>")
        sys.exit(1)

    target = sys.argv[1]
    if target.endswith(".apk") and len(sys.argv) >= 3:
        patch_apk(target, sys.argv[2])
    elif os.path.isdir(target):
        patch_decompiled_dir(target)
    else:
        print(f"Unknown target: {target}")
