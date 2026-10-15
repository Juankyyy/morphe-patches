/*
 * Signature spoof extension for Juanky's Morphe Patches.
 *
 * Adapted from Heval's Morphe Patches
 * (https://github.com/heval99/Heval-Morphe-Patches, GPL-3.0),
 * which ports it from morphe-patches-library.
 */

package app.template.extension.signature;

import android.annotation.SuppressLint;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

/**
 * Every {@code PackageManager.getPackageInfo} call in the patched app is routed here. For the
 * app's own package the returned signatures are replaced with the original signing certificate,
 * so code that hashes its own signature (Firebase's X-Android-Cert header, Google API key
 * restrictions, tamper checks) sees the original app instead of the re-signed one.
 */
@SuppressWarnings("unused")
public final class SignatureSpoof {
    private static final String TAG = "MorpheSignatureSpoof";

    private static Signature spoofedSignature;

    private SignatureSpoof() {
    }

    /** Replaced by the patch with the original package name. */
    public static String getSpoofedPackageName() {
        return "PACKAGE_NAME_PLACEHOLDER";
    }

    /** Replaced by the patch with the original signing certificate (DER, base64). */
    public static String getSpoofedCertificate() {
        return "CERTIFICATE_PLACEHOLDER";
    }

    // Only called for app call sites using the API 33+ Flags overload,
    // which can only execute on API 33+ devices anyway.
    @SuppressLint("NewApi")
    public static PackageInfo getPackageInfo(PackageManager pm, String packageName,
                                             PackageManager.PackageInfoFlags flags)
            throws PackageManager.NameNotFoundException {
        return spoofSignature(pm.getPackageInfo(packageName, flags));
    }

    public static PackageInfo getPackageInfo(PackageManager pm, String packageName, int flags)
            throws PackageManager.NameNotFoundException {
        return spoofSignature(pm.getPackageInfo(packageName, flags));
    }

    private static synchronized Signature signature() {
        if (spoofedSignature == null) {
            spoofedSignature = new Signature(Base64.decode(getSpoofedCertificate(), Base64.DEFAULT));
        }
        return spoofedSignature;
    }

    @SuppressWarnings("deprecation")
    private static PackageInfo spoofSignature(PackageInfo info) {
        try {
            if (info == null || !getSpoofedPackageName().equals(info.packageName)) return info;

            if (info.signatures != null && info.signatures.length > 0) {
                info.signatures[0] = signature();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && info.signingInfo != null) {
                Signature[] signers = info.signingInfo.getApkContentsSigners();
                if (signers != null && signers.length > 0) {
                    signers[0] = signature();
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "spoofSignature failed", t);
        }
        return info;
    }
}
