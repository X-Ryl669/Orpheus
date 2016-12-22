package org.opensilk.music.library.kutr.client;

import android.content.Context;
import android.support.annotation.NonNull;

import java.io.IOException;

/**
 * Created by cyril on 05/12/2016.
 */

public class KutrAccountCredential {
    private final Context context;
    private String account;
    private String passwordHash;
    private String generatedToken;

    public KutrAccountCredential(@NonNull String account, @NonNull String password, @NonNull Context context)
    {
        this.account = account;
        this.passwordHash = password;
        this.context = context;
        generatedToken = "";

    }

    public KutrAccountCredential(@NonNull Context context)
    {
        this.context = context;
        generatedToken = "";

    }


    public String getSelectedAccountName()
    {
        return this.account;
    }

    public String getToken() throws KutrAuthException, IOException
    {
        if (this.account.isEmpty())
            throw new KutrAuthException("Missing credentials to fetch a token");

        return this.generatedToken;
    }

    public String fetchToken(@NonNull String account, @NonNull String password, @NonNull String host) throws KutrAuthException, IOException
    {
        if (host.isEmpty())
            throw new KutrAuthException("Missing host to fetch a token from");

        if (account.isEmpty())
            throw new KutrAuthException("Missing credentials to fetch a token");

        return this.generatedToken;
    }


    public void setCredentials(@NonNull String account, @NonNull String password)
    {
        this.account = account;
        this.passwordHash = password;
        this.generatedToken = "";
    }


}
