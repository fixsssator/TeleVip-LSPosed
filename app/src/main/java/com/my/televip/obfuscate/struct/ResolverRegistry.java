package com.my.televip.obfuscate.struct;

import com.my.televip.Clients.ClientManager;

public class ResolverRegistry {

    Class<?> finalParameter;
    Class<?> clazz;

    public ResolverRegistry(Class<?> clazz){
        this.clazz = clazz;
        try {
            Class<?> parameterResolverClass = null;
            for (Class<?> inner : clazz.getDeclaredClasses()) {
                if (inner.getSimpleName().equals("ParameterResolver")) parameterResolverClass = inner;
            }

            finalParameter = parameterResolverClass;

        } catch (Throwable ignored) {}
    }

    public boolean hasParameter(String name){
        try {
            return (boolean)finalParameter.getMethod("has", String.class).invoke(null, name);
        } catch (Throwable e){
            return false;
        }
    }

    public Class<?>[] resolveParameter(String name){
        try {
            return (Class<?>[]) finalParameter.getMethod("resolve", String.class).invoke(null, name);
        } catch (Throwable e){
            return null;
        }
    }

    public void loadParameter(){
        try {
            clazz.getMethod("loadParameter").invoke(null);
        } catch (Throwable ignored){}
    }

    public static Class<?> getResolverClass() {
        ClientManager.Client clientType = ClientManager.getCurrent();
        if (clientType == null) return null;
        return clientType.getResolverClass();
    }
}