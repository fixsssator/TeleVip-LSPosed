package com.my.televip.dex;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Lists the classes of an installed APK that directly extend a given class, reading only the class
 * table of its dex files (no code, nothing is loaded).
 *
 * Telegram's release builds rename their UI classes in every build, so "the fragment of the Settings
 * screen" cannot be asked for by name. It can be found by what it is: a direct subclass of BaseFragment
 * (known through the readable ProfileActivity) with a particular shape. This gives the subclasses; the
 * shape is checked by reflection on the few hundred that come out.
 */
public final class DexNames {

    private DexNames() {}

    /** Dotted names of classes whose direct superclass has the given descriptor, e.g. "Lorg/telegram/ui/ActionBar/s2;". */
    public static List<String> subclassesInApks(List<String> apkPaths, String superDescriptor) {
        List<String> out = new ArrayList<>();
        for (String path : apkPaths) {
            try (ZipFile zip = new ZipFile(path)) {
                Enumeration<? extends ZipEntry> en = zip.entries();
                while (en.hasMoreElements()) {
                    ZipEntry e = en.nextElement();
                    String n = e.getName();
                    if (!n.matches("classes\\d*\\.dex")) continue;
                    try (InputStream in = zip.getInputStream(e)) {
                        out.addAll(subclassesOf(readAll(in, (int) Math.max(e.getSize(), 1 << 20)), superDescriptor));
                    }
                }
            } catch (Throwable ignored) {
                // a split APK without code, unreadable file: nothing to add
            }
        }
        return out;
    }

    public static List<String> subclassesOf(byte[] dex, String superDescriptor) {
        List<String> out = new ArrayList<>();
        ByteBuffer b = ByteBuffer.wrap(dex).order(ByteOrder.LITTLE_ENDIAN);
        if (dex.length < 112 || b.getInt(0) != 0x0A786564 /* "dex\n" */) return out;

        int stringIdsSize = b.getInt(56), stringIdsOff = b.getInt(60);
        int typeIdsSize = b.getInt(64), typeIdsOff = b.getInt(68);
        int classDefsSize = b.getInt(96), classDefsOff = b.getInt(100);

        int superType = -1;
        for (int t = 0; t < typeIdsSize; t++) {
            if (superDescriptor.equals(string(b, stringIdsOff, b.getInt(typeIdsOff + 4 * t)))) {
                superType = t;
                break;
            }
        }
        if (superType < 0) return out;

        for (int i = 0; i < classDefsSize; i++) {
            int def = classDefsOff + 32 * i;
            if (b.getInt(def + 8) != superType) continue;           // superclass_idx
            String desc = string(b, stringIdsOff, b.getInt(typeIdsOff + 4 * b.getInt(def)));
            if (desc.length() > 2 && desc.charAt(0) == 'L' && desc.endsWith(";")) {
                out.add(desc.substring(1, desc.length() - 1).replace('/', '.'));
            }
        }
        return out;
    }

    private static String string(ByteBuffer b, int stringIdsOff, int idx) {
        int p = b.getInt(stringIdsOff + 4 * idx);
        while ((b.get(p) & 0x80) != 0) p++;     // skip the ULEB128 length
        p++;
        int start = p;
        while (b.get(p) != 0) p++;
        byte[] raw = new byte[p - start];
        for (int i = 0; i < raw.length; i++) raw[i] = b.get(start + i);
        return new String(raw, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static byte[] readAll(InputStream in, int hint) throws java.io.IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(hint);
        byte[] buf = new byte[1 << 16];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        return bos.toByteArray();
    }
}
