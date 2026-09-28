package com.my.televip.obfuscate;

import com.my.televip.Clients.ClientManager;
import com.my.televip.obfuscate.struct.ClientObfuscationData;
import com.my.televip.utils.JsonAssetReader;

import java.util.EnumMap;
import java.util.Map;

public class ObfuscationManager {

    private static final Map<ClientManager.Client, ClientObfuscationData> cache =
            new EnumMap<>(ClientManager.Client.class);

    private static volatile ClientObfuscationData currentClientData;
    private static volatile boolean attemptedLoad = false;

    public static ClientObfuscationData current() {
        if (currentClientData != null) return currentClientData;
        if (attemptedLoad) return null;

        synchronized (ObfuscationManager.class) {
            if (currentClientData != null) return currentClientData;
            attemptedLoad = true;

            ClientManager.Client type = ClientManager.getCurrent();
            if (type == null) return null;

            currentClientData = load(type);
            return currentClientData;
        }
    }

    private static ClientObfuscationData load(ClientManager.Client type) {
        if (cache.containsKey(type)) return cache.get(type);

        ClientObfuscationData data = null;
        String clientPath = "assets/clients/" + type.name() + ".json";
        String clientJson = JsonAssetReader.readRaw(clientPath, false);

        String clientAliasPath = "assets/clients/Alias/" + type.name() + ".json";
        String clientAliasJson = JsonAssetReader.readRaw(clientAliasPath, false);

        if (clientJson != null && clientAliasJson != null)
            data = ClientObfuscationData.parse(clientJson, clientAliasJson);

        if (data != null)
            cache.put(type, data);
        return data;
    }

}