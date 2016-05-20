package com.example.streaming;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;
public class HttpParser {

	private final static String TAG = "HttpParser";
	private final static String RANGE_PARAMS = "Range: bytes=";
	private final static String CONTENT_RANGE_PARAMS = "Content-Range: bytes ";
	private final static String CONTENT_LENGTH_PARAMS = "Content-Length: ";
	private final static String HTTP_BODY_END = "\r\n\r\n";
	private final static String HTTP_RESPONSE_BEGIN = "HTTP/";
	private final static String HTTP_DOCUMENT_BEGIN = "<html>";
	private final static String HTTP_REQUEST_BEGIN = "GET ";
	private static final int HEADER_BUFFER_LENGTH_MAX = 1448 * 50;
	private byte[] headerBuffer = new byte[HEADER_BUFFER_LENGTH_MAX];
	private int headerBufferLength = 0;
	private int remotePort = -1;
	private String remoteHost;
	private int localPort;
	private String localHost;

	public HttpParser(String rHost, int rPort, String lHost, int lPort) {
		remoteHost = rHost;
		remotePort = rPort;
		localHost = lHost;
		localPort = lPort;
	}

	static public class ProxyRequest {
		public String _body;
		public long _rangePosition;
		public boolean _overRange;
	}

	static public class ProxyResponse {
		public byte[] _body;
		public byte[] _other;
		public long _currentPosition;
		public long _duration;
	}

	public void clearHttpBody() {
		headerBuffer = new byte[HEADER_BUFFER_LENGTH_MAX];
		headerBufferLength = 0;
	}

	public byte[] getRequestBody(byte[] source, int length) { 
		List<byte[]> httpRequest = getHttpBody(HTTP_REQUEST_BEGIN,HTTP_BODY_END, source, length);
		if (httpRequest.size() > 0){
			return httpRequest.get(0);
		}
		return null;
	}

	
	public ProxyRequest getProxyRequest(byte[] bodyBytes, long urlsize) {
		ProxyRequest result = new ProxyRequest();
		result._body = new String(bodyBytes);//127.0.0.1
		result._body = result._body.replace(localHost, remoteHost);//  127.0.0.1 => mac9.iptime.org
		if (remotePort == -1)
			result._body = result._body.replace(":" + localPort, "");
		else
			result._body = result._body.replace(":" + localPort, ":" + remotePort);
		String rangePosition = "0";
		if (result._body.contains(RANGE_PARAMS) == false) {
			result._rangePosition = 0;
		} else {
			rangePosition = getSubString(result._body, RANGE_PARAMS, "-");
			try {
				Log.d(TAG, "------->rangePosition:" + rangePosition);
				result._rangePosition = Integer.valueOf(rangePosition);
				if ((result._rangePosition >= urlsize) && (urlsize > 0)) {
					result._body = result._body.replaceAll(RANGE_PARAMS	+ rangePosition, RANGE_PARAMS + "0");
					result._rangePosition = 0;
					result._overRange = true;
				}
			} catch (Exception e) {
				result._rangePosition = 0;
				e.printStackTrace();
			}
		}
		Log.d(TAG, result._body);
		return result;
	}
 
	public ProxyResponse getProxyResponse(byte[] source, int length) {
		List<byte[]> httpResponse = getHttpBody(HTTP_RESPONSE_BEGIN,HTTP_BODY_END, source, length);
		if (httpResponse.size() == 0)
			return null;
		ProxyResponse result = new ProxyResponse();
		result._body = httpResponse.get(0);
		String text = new String(result._body);
		Log.d(TAG + "<---", text);
		if (httpResponse.size() == 2)
			result._other = httpResponse.get(1);
		try {
			if (text.contains(CONTENT_RANGE_PARAMS) == false) { // Content Range  Á¶Á¤
				result._currentPosition = 0;
				if (text.contains(CONTENT_LENGTH_PARAMS) == true) {
					String duration = getSubString(text, CONTENT_LENGTH_PARAMS, "\r\n");
					try {
						result._duration = Integer.valueOf(duration) - 1;
					} catch (Exception e) {
						result._duration = 0;
						e.printStackTrace();
					}
				} else {
					result._duration = 0;
				}
			} else {
				String currentPosition = getSubString(text,CONTENT_RANGE_PARAMS, "-");
				try {
					result._currentPosition = Integer.valueOf(currentPosition);
					String startStr = CONTENT_RANGE_PARAMS + currentPosition + "-";
					String duration = getSubString(text, startStr, "/");
					result._duration = Integer.valueOf(duration);
				} catch (Exception e) {
					result._currentPosition = 0;
					result._duration = 0;
					e.printStackTrace();
				}
			}
		} catch (Exception ex) {
			Log.e(TAG, getExceptionMessage(ex));
		}
		return result;
	}
	
	public String getSubString(String source, String startStr,String endStr) {
		int startIndex = source.indexOf(startStr) + startStr.length();
		int endIndex = source.indexOf(endStr, startIndex);
		return source.substring(startIndex, endIndex);
	}
	
	
	public static String getExceptionMessage(Exception ex) {
		String result = "";
		StackTraceElement[] stes = ex.getStackTrace();
		for (int i = 0; i < stes.length; i++) {
			result = result + stes[i].getClassName() + "."
					+ stes[i].getMethodName() + "  " + stes[i].getLineNumber()
					+ "line" + "\r\n";
		}
		return result;
	}

	private List<byte[]> getHttpBody(String beginStr, String endStr, byte[] source, int length) {
		if ((headerBufferLength + length) >= headerBuffer.length) {
			clearHttpBody();
		}
		System.arraycopy(source, 0, headerBuffer, headerBufferLength, length);
		headerBufferLength += length;
		List<byte[]> result = new ArrayList<byte[]>();
		String responseStr = new String(headerBuffer);
		if (responseStr.contains(beginStr) && responseStr.contains(endStr)) {
			int startIndex = responseStr.indexOf(beginStr, 0);
			int endIndex = responseStr.indexOf(endStr, startIndex);
			endIndex += endStr.length();
			byte[] header = new byte[endIndex - startIndex];
			System.arraycopy(headerBuffer, startIndex, header, 0, header.length);
			result.add(header);
			if ((headerBufferLength > header.length) && (responseStr.indexOf(HTTP_DOCUMENT_BEGIN, header.length) == -1)) {
				byte[] other = new byte[headerBufferLength - header.length];
				System.arraycopy(headerBuffer, header.length, other, 0,	other.length);
				result.add(other);
			}
			clearHttpBody();
		}
		return result;
	}

}
