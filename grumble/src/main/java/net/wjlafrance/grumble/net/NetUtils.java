package net.wjlafrance.grumble.net;

import java.io.IOException;
import java.io.InputStream;

public class NetUtils {

	// https://github.com/mumble-voip/mumble/blob/master/src/PacketDataStream.h
	public static int readVarint(InputStream inputStream) throws IOException {
		int ret = inputStream.read();
		if ((ret & 0x80) == 0x00) { // 0xxxxxxx
			return (ret & 0x7F);
		} else if ((ret & 0xC0) == 0x80) { // 1xxxxxxx
			return (ret & 0x3F) << 8 | inputStream.read();
		} else if ((ret & 0xE0) == 0xC0) { // 11xxxxxx
			return (ret & 0x1F) << 16 | inputStream.read() << 8 | inputStream.read();
		} else if ((ret & 0xF0) == 0xE0) { // 111xxxxx
			return (ret & 0x0F) << 24 | inputStream.read() << 16 | inputStream.read() << 8 | inputStream.read();
		} else if ((ret & 0xF0) == 0xF0) { // 11110000
			switch ((int) (ret & 0xFC)) { // first 6 bits
				case 0xF0: // 111100xx - 32bit positive integer
					return inputStream.read() << 24 | inputStream.read() << 16 | inputStream.read() << 8 | inputStream.read();
//				case 0xF4: // 111101xx - 64bit number
//					return inputStream.read() << 56 | inputStream.read() << 48 | inputStream.read() << 40 | inputStream.read() << 32
//							| inputStream.read() << 24 | inputStream.read() << 16 | inputStream.read() << 8 | inputStream.read();
				case 0xFC: // 111111xx byte-inverted negative two-byte number
					return ~(ret & 0x03);
			}
		}
		return 0;
	}

}
