package gitlet;

import java.io.File;
import java.io.Serializable;

public class Blob implements Serializable {


    private File file;
    private String id;
    private String contents;

    //TODO: Blob constructor needs Javadoc
    public Blob(File file) {
        this.file = file;

        this.contents = Utils.readContentsAsString(file);

        id = Utils.sha1((Object) Utils.serialize(this));

        File blobFile = Utils.join(Dir.BLOBS, id + ".txt");
        Utils.writeObject(blobFile, this);
    }

    public File getFile() {
        return file;
    }

    public String getId() {
        return id;
    }

    public String getContents() {
        return contents;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Blob)) {
            return false;
        }

        Blob other = (Blob) o;
        return this.getId().equals(other.getId());
    }
}
