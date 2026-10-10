package com.focus.app.service

import android.telecom.Call
import android.telecom.CallScreeningService

/**
 * Stub. Lets the app request the call-screening role (A1). It allows every call
 * until the allowed-callers list (A8) is built.
 */
class FocusCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        respondToCall(callDetails, CallResponse.Builder().build())
    }
}
