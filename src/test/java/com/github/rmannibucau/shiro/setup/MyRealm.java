package com.github.rmannibucau.shiro.setup;

import org.apache.shiro.realm.SimpleAccountRealm;

import javax.enterprise.context.Dependent;

@Dependent
public class MyRealm extends SimpleAccountRealm {
    public MyRealm() {
        addAccount("user", "pwd", "rtest");
        getUser("user").addStringPermission("ptest");
    }
}