package org.opensilk.music.library.kutr.client;

import java.net.URI;
import android.support.annotation.NonNull;

/**
 * Created by cyril on 05/12/2016.
 */

public class Item {
    private final String type;
    private final String id;
    private final String name;
    private final String mimeType;
    private final String downloadURL;

    /** The URI looks like : kutr://type/id/name#realURL */
    public Item(@NonNull String _uri, @NonNull String mimetype)
    {
        URI uri = URI.create(_uri);
        type = uri.getAuthority();
        String path = uri.getPath();
        id = path.substring(0, path.indexOf("/"));
        name = path.substring(path.indexOf("/"));
        downloadURL = uri.getFragment();
        this.mimeType = mimetype;
    }

    public String getMimeType() { return this.mimeType; }

    public String getUri() { return "kutr://" + type + "/" + id + "/" + name + (downloadURL.isEmpty() ? "#" + downloadURL : ""); }

    public String getId()  { return id; }

    public String getName() { return name; }

    public String getDownloadUrl() { return downloadURL; }

}
