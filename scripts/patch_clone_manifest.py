import sys
import os
import struct
import zipfile

AUTHORITY_OR_PERMISSION_PATTERNS = [
    "com.instagram.barcelona",
    "com.instagram.barcelona.permission.SYSTEM_ONLY",
    "com.instagram.barcelona.permission.CROSS_PROCESS_BROADCAST_MANAGER",
    "com.instagram.barcelona.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
    "com.instagram.barcelona.securefileprovider",
    "com.instagram.barcelona.fileprovider",
    "com.instagram.barcelona.androidx-startup",
    "com.instagram.barcelona.liteprovider.BarcelonaLiteContentProvider",
    "com.instagram.barcelona.liteprovider.usdid.UsdidValuesProvider",
    "com.instagram.barcelona.provider.AccessLibraryContentProvider",
    "com.instagram.barcelona.hatch.chat.files",
    "com.instagram.barcelona.hatch.files",
    "com.instagram.barcelona.provider.phoneid",
]

def should_rename_string(s: str) -> bool:
    return s in AUTHORITY_OR_PERMISSION_PATTERNS

def patch_manifest_bytes(raw_data: bytes, old_prefix="com.instagram.barcelona", new_prefix="com.instagram.barcelonp") -> bytes:
    assert len(old_prefix) == len(new_prefix), f"Prefix lengths must match: {len(old_prefix)} != {len(new_prefix)}"
    data = bytearray(raw_data)
    
    if len(data) < 36:
        print("[!] Warning: Manifest data too short, skipping StringPool patch")
        return raw_data

    # Parse AXML header
    res_type, header_size, file_size = struct.unpack("<HHI", data[:8])
    if res_type != 0x0003:
        print(f"[!] Warning: Not a valid AXML header (type={res_type:#x})")
        return raw_data

    sp_type, sp_header_size, sp_chunk_size, string_count, style_count, flags, strings_start, styles_start = struct.unpack("<HHIIIIII", data[8:36])
    if sp_type != 0x0001:
        print(f"[!] Warning: StringPool chunk not found at expected offset (type={sp_type:#x})")
        return raw_data

    is_utf8 = bool(flags & (1 << 8))
    offset_data = data[36:36 + 4 * string_count]
    offsets = [struct.unpack("<I", offset_data[i*4:(i+1)*4])[0] for i in range(string_count)]
    str_base = 8 + strings_start

    renamed_count = 0
    for idx, off in enumerate(offsets):
        p = str_base + off
        if is_utf8:
            p_cur = p
            c_len = data[p_cur]
            p_cur += 1
            if c_len & 0x80:
                p_cur += 1
            b_len = data[p_cur]
            p_cur += 1
            if b_len & 0x80:
                p_cur += 1
            s_bytes = bytes(data[p_cur:p_cur + b_len])
            s = s_bytes.decode("utf-8", errors="replace")
            if should_rename_string(s):
                new_s = s.replace(old_prefix, new_prefix)
                new_bytes = new_s.encode("utf-8")
                if len(new_bytes) == len(s_bytes):
                    data[p_cur:p_cur + b_len] = new_bytes
                    print(f"  [+] Renamed StringPool UTF-8 [{idx}]: {s} -> {new_s}")
                    renamed_count += 1
        else:
            # UTF-16LE
            u_len = struct.unpack("<H", data[p:p+2])[0]
            s_bytes = bytes(data[p+2:p+2 + u_len*2])
            s = s_bytes.decode("utf-16le", errors="replace")
            if should_rename_string(s):
                new_s = s.replace(old_prefix, new_prefix)
                new_bytes = new_s.encode("utf-16le")
                if len(new_bytes) == len(s_bytes):
                    data[p+2:p+2 + u_len*2] = new_bytes
                    print(f"  [+] Renamed StringPool UTF-16 [{idx}]: {s} -> {new_s}")
                    renamed_count += 1

    print(f"[✔] Total StringPool entries renamed: {renamed_count}")
    return bytes(data)

def patch_apk(input_apk: str, output_apk: str):
    print(f"[*] Patching APK: {input_apk} -> {output_apk}")
    with zipfile.ZipFile(input_apk, "r") as zin, zipfile.ZipFile(output_apk, "w") as zout:
        for item in zin.infolist():
            data = zin.read(item.filename)
            if item.filename == "AndroidManifest.xml":
                data = patch_manifest_bytes(data)
            zout.writestr(item, data)
    print(f"[✔] Successfully created cloned base APK: {output_apk}")

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python patch_clone_manifest.py <input.apk> <output.apk>")
        sys.exit(1)

    patch_apk(sys.argv[1], sys.argv[2])
