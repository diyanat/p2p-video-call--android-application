package com.phoenix.p2pcamserver.spydroid.api;

import com.phoenix.p2pcamserver.streaming.rtsp.RtspServer;

public class CustomRtspServer extends RtspServer {
	public CustomRtspServer() {
		super();
		// RTSP server disabled by default
		mEnabled = true;
	}
}

