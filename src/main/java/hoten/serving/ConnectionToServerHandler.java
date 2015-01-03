package hoten.serving;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

public abstract class ConnectionToServerHandler extends SocketHandler {

    public final File localDataFolder;

    public ConnectionToServerHandler(Socket socket) throws IOException {
        super(socket);
        localDataFolder = new File(_in.readUTF()); // :(
        localDataFolder.mkdirs();
    }

    public void start() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                respondToHashes();
                readNewFilesFromServer();
                onConnectionSettled();
                processDataUntilClosed();
            } catch (IOException ex) {
                Logger.getLogger(ConnectionToServerHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }

    private void respondToHashes() throws IOException {
        Map<String, byte[]> hashes = readFileHashesFromServer();
        Collection<File> localFiles = FileUtils.listFiles(localDataFolder, null, true);
        Collection<String> filesToRequest = compareFileHashes(localFiles, hashes);
        _out.writeUTF(new Gson().toJson(filesToRequest, List.class));
        localFiles.stream().forEach((f) -> {
            f.delete();
        });
    }

    private Map<String, byte[]> readFileHashesFromServer() throws IOException {
        String jsonHashes = _in.readUTF();
        Type type = new TypeToken<Map<String, byte[]>>() {
        }.getType();
        return new Gson().fromJson(jsonHashes, type);
    }

    private Collection<String> compareFileHashes(Collection<File> files, Map<String, byte[]> hashes) {
        List<String> filesToRequest = new ArrayList();
        hashes.forEach((fileName, fileHash) -> {
            try {
                File f = new File(localDataFolder, fileName);

                for (File cf : files) {
                    if (cf.equals(f)) {
                        //remove this file as a candidate for pruning
                        files.remove(cf);
                        break;
                    }
                }

                if (!f.exists() || !Arrays.equals(DigestUtils.md5(FileUtils.readFileToString(f)), fileHash)) {
                    filesToRequest.add(fileName);
                }
            } catch (IOException ex) {
                Logger.getLogger(ConnectionToServerHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        return filesToRequest;
    }

    private void readNewFilesFromServer() throws IOException {
        int numFiles = _in.readInt();
        for (int i = 0; i < numFiles; i++) {
            String fileName = _in.readUTF();
            int length = _in.readInt();
            Logger.getLogger(ConnectionToServerHandler.class.getName()).log(Level.INFO, "Updating {0}, size = {1}", new Object[]{fileName, length});
            byte[] data = new byte[length];
            _in.readFully(data);
            FileUtils.writeByteArrayToFile(new File(localDataFolder, fileName), data);
        }
        _out.write(0);//done updating files
    }
}
