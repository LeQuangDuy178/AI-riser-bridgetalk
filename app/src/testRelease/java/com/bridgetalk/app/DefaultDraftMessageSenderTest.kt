package com.bridgetalk.app

// The legacy release test that asserted the "production backend" rejection
// has been removed together with the rejection. The release build's send
// path is now exercised by ConfirmDraftMessageSenderTest in the shared
// test source set (app/src/test/.../sender/ConfirmDraftMessageSenderTest.kt).
