import sys
import struct
import zipfile

def verify_apk(apk_path: str):
    print(f"[*] Verifying {apk_path}...")
    with open(apk_path, "rb") as f:
        data = f.read()

    eocd_pos = data.rfind(b"\x50\x4b\x05\x06")
    if eocd_pos == -1:
        print("[!] No EOCD found")
        sys.exit(1)

    disk_num, cd_disk, disk_entries, total_entries, cd_size, cd_offset = struct.unpack("<HHHHII", data[eocd_pos+4:eocd_pos+20])
    cd_data = data[cd_offset:cd_offset+cd_size]

    pos = 0
    checked = 0
    errors = 0

    # 1. Verify root DEX files and BarcelonaAppShell existence
    dex_count = 0
    has_app_shell = False
    with zipfile.ZipFile(apk_path, "r") as z:
        for name in z.namelist():
            if name.startswith("classes") and name.endswith(".dex"):
                dex_count += 1
                content = z.read(name)
                if b"BarcelonaAppShell" in content:
                    has_app_shell = True
                    print(f"  [✔] Found BarcelonaAppShell inside root DEX: {name}")

    print(f"  Total root DEX files: {dex_count}")
    if dex_count == 0:
        print("  [!] ERROR: No root classes.dex found!")
        errors += 1
    if not has_app_shell:
        print("  [!] ERROR: BarcelonaAppShell is missing from root DEX files!")
        errors += 1
    else:
        print("  [✔] Multidex & BarcelonaAppShell verification passed.")

    # 2. Verify 4K page-alignment for uncompressed .so libraries
    while pos < len(cd_data):
        if cd_data[pos:pos+4] != b"\x50\x4b\x01\x02":
            break
        comp_method = struct.unpack("<H", cd_data[pos+10:pos+12])[0]
        comp_size, uncomp_size = struct.unpack("<II", cd_data[pos+20:pos+28])
        fn_len, extra_len, comment_len = struct.unpack("<HHH", cd_data[pos+28:pos+34])
        local_offset = struct.unpack("<I", cd_data[pos+42:pos+46])[0]
        fn = cd_data[pos+46:pos+46+fn_len].decode("utf-8", errors="ignore")

        if fn.endswith(".so") or "origin.apk" in fn:
            # Check local file header to find exact data offset
            loc_fn_len, loc_extra_len = struct.unpack("<HH", data[local_offset+26:local_offset+30])
            data_offset = local_offset + 30 + loc_fn_len + loc_extra_len
            mod_4k = data_offset % 4096
            if comp_method == 0 and mod_4k != 0:
                print(f"  [!] ERROR: Uncompressed file {fn} is NOT 4K page-aligned! (offset={data_offset}, mod_4k={mod_4k})")
                errors += 1
            else:
                checked += 1

        pos += 46 + fn_len + extra_len + comment_len

    if errors > 0:
        print(f"[✘] Verification FAILED with {errors} errors!")
        sys.exit(1)
    else:
        print(f"[✔] Verification PASSED ({checked} native libraries checked, {dex_count} DEX files verified).")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python verify_apk_alignment.py <apk_path>")
        sys.exit(1)
    for path in sys.argv[1:]:
        verify_apk(path)
