package net.omplanet.androidcontactsapp.ui;

import android.app.Application;

import com.facebook.model.GraphUser;

import java.util.List;

/**
 * Use a custom Application class to pass state data between Activities.
 */
public class MyApplication extends Application {

    private List<GraphUser> selectedUsers;

    public List<GraphUser> getSelectedUsers() {
        return selectedUsers;
    }

    public void setSelectedUsers(List<GraphUser> users) {
        selectedUsers = users;
    }
}
