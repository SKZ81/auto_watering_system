package com.skz81.simplenfc2http;

import android.nfc.tech.Ndef;

public interface NdefTagCallback {
    void onNDEFDiscovered(Ndef ndef);
}
