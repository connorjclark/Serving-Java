package hoten.serving {
	/**
	 * ServingSocket.as
	 * 
	 * Processes data coming from a server, and passes off the data it reads to an external function.
	 * 
	 * @author Hoten
	 */
	
	import flash.errors.*;
	import flash.events.*;
	import flash.net.Socket;

	public class ServingSocket extends Socket {
		
		private var curBuffer:int = -1;
		private var handleData:Function;

		public function ServingSocket(host:String, port:uint, handleData:Function) {
			super(host, port);
			this.handleData = handleData;
			addEventListener(ProgressEvent.SOCKET_DATA, readResponse);
		}
		
		public function write(msg:ServerMessage):void {
			try {
				writeByte(msg.length >> 16);
                writeByte(msg.length >> 8);
                writeByte(msg.length);
                writeByte(msg.id);
				writeBytes(msg);
				flush();
			} catch (e:IOError) {
				trace(e);
			}
		}
		
		private function readResponse(e:ProgressEvent):void {
			while (true) {
				if (bytesAvailable >= 4 && curBuffer == -1) curBuffer = (readUnsignedByte() << 16) + (readUnsignedByte() << 8) + readUnsignedByte();
				if (bytesAvailable >= curBuffer + 1 && curBuffer != -1) {
					var msg:ServerMessage = new ServerMessage(readUnsignedByte());
					if (curBuffer > 0) readBytes(msg, 0, curBuffer);
					handleData(msg);
					curBuffer = -1;
					if (bytesAvailable <= 4) break;
				}else break;
			}
		}
	}
}