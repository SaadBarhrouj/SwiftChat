package Server.utils;

import java.io.File;


public final class AppPaths {


    public static final String SERVER_UPLOADS_DIR =
            "src" + File.separator + "server" + File.separator + "data" + File.separator + "uploads" + File.separator;


    public static final String SERVER_CONTACTS_SERIALIZATION_DIR =
            "src" + File.separator + "server" + File.separator + "data" + File.separator + "contacts" + File.separator;


    public static final String LEGACY_OR_SPECIFIC_GROUP_UPLOADS_DIR =
            "src" + File.separator + "uploads" + File.separator;


    private AppPaths() {
    }

}