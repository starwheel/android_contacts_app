package net.omplanet.androidcontactsapp.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.widget.Toast;

import com.facebook.FacebookException;
import com.facebook.widget.FriendPickerFragment;
import com.facebook.widget.PickerFragment;

import net.omplanet.androidcontactsapp.R;

/**
 * The PickerActivity enhances the Friend Picker by adding a title
 * and a Done button. The selection results are saved in the MyApplication
 * instance.
 */
public class PickerActivity extends FragmentActivity {
    public static final Uri FRIEND_PICKER = Uri.parse("picker://friend");
    private FriendPickerFragment friendPickerFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pickers);

        Bundle args = getIntent().getExtras();
        FragmentManager manager = getSupportFragmentManager();
        Fragment fragmentToShow;

        if (savedInstanceState == null) {
            friendPickerFragment = new FriendPickerFragment(args);
            friendPickerFragment.setMultiSelect(false);
            friendPickerFragment.setFriendPickerType(FriendPickerFragment.FriendPickerType.TAGGABLE_FRIENDS);
        } else {
            friendPickerFragment = (FriendPickerFragment) manager.findFragmentById(R.id.picker_fragment);
        }

        friendPickerFragment.setOnErrorListener(new PickerFragment.OnErrorListener() {
            @Override
            public void onError(PickerFragment<?> fragment, FacebookException error) {
                PickerActivity.this.onError(error);
            }
        });
        friendPickerFragment.setOnDoneButtonClickedListener(new PickerFragment.OnDoneButtonClickedListener() {
            @Override
            public void onDoneButtonClicked(PickerFragment<?> fragment) {
                finishActivity();
            }
        });
        fragmentToShow = friendPickerFragment;

        manager.beginTransaction().replace(R.id.picker_fragment, fragmentToShow).commit();
    }

    @Override
    protected void onStart() {
        super.onStart();

        try {
            friendPickerFragment.loadData(false);
        } catch (Exception ex) {
            onError(ex);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void onError(Exception error) {
        Toast toast = Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT);
        toast.show();
    }

    private void onError(String error, final boolean finishActivity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.error_dialog_title).
                setMessage(error).
                setPositiveButton(R.string.error_dialog_button_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (finishActivity) {
                            finishActivity();
                        }
                    }
                });
        builder.show();
    }

    private void finishActivity() {
        if (friendPickerFragment != null) {
            MyApplication app = (MyApplication) getApplication();
            app.setSelectedUsers(friendPickerFragment.getSelection());
        }

        setResult(RESULT_OK, null);
        finish();
    }
}
