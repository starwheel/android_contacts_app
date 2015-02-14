package net.omplanet.androidcontactsapp.ui;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Contacts.Photo;
import android.provider.ContactsContract.Data;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.omplanet.androidcontactsapp.BuildConfig;
import net.omplanet.androidcontactsapp.R;
import net.omplanet.androidcontactsapp.util.ImageLoader;
import net.omplanet.androidcontactsapp.util.Utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

/**
 * This fragment displays details of a specific contact from the contacts provider. It shows the
 * contact's display photo, name and all its mailing addresses. You can also modify this fragment
 * to show other information, such as phone numbers, email addresses and so forth.
 *
 * This fragment appears full-screen in an activity on devices with small screen sizes, and as
 * part of a two-pane layout on devices with larger screens, alongside the
 * {@link ContactsListFragment}.
 *
 * To create an instance of this fragment, use the factory method
 * {@link ContactDetailFragment#newInstance(android.net.Uri)}, passing as an argument the contact
 * Uri for the contact you want to display.
 */
public class ContactDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String EXTRA_CONTACT_URI =
            "com.example.android.contactslist.ui.EXTRA_CONTACT_URI";

    // Defines a tag for identifying log entries
    private static final String TAG = "ContactDetailFragment";

    // The geo Uri scheme prefix, used with Intent.ACTION_VIEW to form a geographical address
    // intent that will trigger available apps to handle viewing a location (such as Maps)
    private static final String GEO_URI_SCHEME_PREFIX = "geo:0,0?q=";
    public static final int PICK_FACEBOOK_USER_REQUEST = 200;
    public static final int PICK_TWITTER_USER_REQUEST = 201;

    // Whether or not this fragment is showing in a two pane layout
    private boolean mIsTwoPaneLayout;

    private Uri mContactUri; // Stores the contact Uri for this fragment instance
    private ImageLoader mImageLoader; // Handles loading the contact image in a background thread

    // Used to store references to key views, layouts and menu items as these need to be updated
    // in multiple methods throughout this class.
    private ImageView mImageView;
    private LinearLayout mSocialNetworksLayout;
    private LinearLayout mPhoneNumbersLayout;
    private LinearLayout mAddressesLayout;
    private TextView mEmptyView;
    private TextView mContactName;
    private MenuItem mEditContactMenuItem;

    /**
     * Factory method to generate a new instance of the fragment given a contact Uri. A factory
     * method is preferable to simply using the constructor as it handles creating the bundle and
     * setting the bundle as an argument.
     *
     * @param contactUri The contact Uri to load
     * @return A new instance of {@link ContactDetailFragment}
     */
    public static ContactDetailFragment newInstance(Uri contactUri) {
        // Create new instance of this fragment
        final ContactDetailFragment fragment = new ContactDetailFragment();

        // Create and populate the args bundle
        final Bundle args = new Bundle();
        args.putParcelable(EXTRA_CONTACT_URI, contactUri);

        // Assign the args bundle to the new fragment
        fragment.setArguments(args);

        // Return fragment
        return fragment;
    }

    /**
     * Fragments require an empty constructor.
     */
    public ContactDetailFragment() {}

    /**
     * Sets the contact that this Fragment displays, or clears the display if the contact argument
     * is null. This will re-initialize all the views and start the queries to the system contacts
     * provider to populate the contact information.
     *
     * @param contactLookupUri The contact lookup Uri to load and display in this fragment. Passing
     *                         null is valid and the fragment will display a message that no
     *                         contact is currently selected instead.
     */
    public void setContact(Uri contactLookupUri) {

        // In version 3.0 and later, stores the provided contact lookup Uri in a class field. This
        // Uri is then used at various points in this class to map to the provided contact.
        if (Utils.hasHoneycomb()) {
            mContactUri = contactLookupUri;
        } else {
            // For versions earlier than Android 3.0, stores a contact Uri that's constructed from
            // contactLookupUri. Later on, the resulting Uri is combined with
            // Contacts.Data.CONTENT_DIRECTORY to map to the provided contact. It's done
            // differently for these earlier versions because Contacts.Data.CONTENT_DIRECTORY works
            // differently for Android versions before 3.0.
            mContactUri = Contacts.lookupContact(getActivity().getContentResolver(),
                    contactLookupUri);
        }

        // If the Uri contains data, load the contact's image and load contact details.
        if (contactLookupUri != null) {
            // Asynchronously loads the contact image
            mImageLoader.loadImage(mContactUri, mImageView);

            // Shows the contact photo ImageView and hides the empty view
            mImageView.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);

            // Shows the edit contact action/menu item
            if (mEditContactMenuItem != null) {
                mEditContactMenuItem.setVisible(true);
            }

            // Starts 3 queries to to retrieve contact information from the Contacts Provider.
            // restartLoader() is used instead of initLoader() as this method may be called
            // multiple times.
            getLoaderManager().restartLoader(ContactDetailQuery.QUERY_ID, null, this);
            getLoaderManager().restartLoader(ContactPhoneNumberQuery.QUERY_ID, null, this);
            getLoaderManager().restartLoader(ContactAddressQuery.QUERY_ID, null, this);
        } else {
            // If contactLookupUri is null, then the method was called when no contact was selected
            // in the contacts list. This should only happen in a two-pane layout when the user
            // hasn't yet selected a contact. Don't display an image for the contact, and don't
            // account for the view's space in the layout. Turn on the TextView that appears when
            // the layout is empty, and set the contact name to the empty string. Turn off any menu
            // items that are visible.
            mImageView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
            mSocialNetworksLayout.removeAllViews();
            mPhoneNumbersLayout.removeAllViews();
            mAddressesLayout.removeAllViews();
            if (mContactName != null) {
                mContactName.setText("");
            }
            if (mEditContactMenuItem != null) {
                mEditContactMenuItem.setVisible(false);
            }
        }
    }

    /**
     * When the Fragment is first created, this callback is invoked. It initializes some key
     * class fields.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if this fragment is part of a two pane set up or a single pane
        mIsTwoPaneLayout = getResources().getBoolean(R.bool.has_two_panes);

        // Let this fragment contribute menu items
        setHasOptionsMenu(true);

        /*
         * The ImageLoader takes care of loading and resizing images asynchronously into the
         * ImageView. More thorough sample code demonstrating background image loading as well as
         * details on how it works can be found in the following Android Training class:
         * http://developer.android.com/training/displaying-bitmaps/
         */
        mImageLoader = new ImageLoader(getActivity(), getLargestScreenDimension()) {
            @Override
            protected Bitmap processBitmap(Object data) {
                // This gets called in a background thread and passed the data from
                // ImageLoader.loadImage().
                return loadContactPhoto((Uri) data, getImageSize());
            }
        };

        // Set a placeholder loading image for the image loader
        mImageLoader.setLoadingImage(R.drawable.ic_contact_picture_180_holo_light);

        // Tell the image loader to set the image directly when it's finished loading
        // rather than fading in
        mImageLoader.setImageFadeIn(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflates the main layout to be used by this fragment
        final View detailView =
                inflater.inflate(R.layout.contact_detail_fragment, container, false);

        // Gets handles to view objects in the layout
        mImageView = (ImageView) detailView.findViewById(R.id.contact_image);
        mSocialNetworksLayout = (LinearLayout) detailView.findViewById(R.id.social_networks_layout);
        mPhoneNumbersLayout = (LinearLayout) detailView.findViewById(R.id.contact_details_layout1);
        mAddressesLayout = (LinearLayout) detailView.findViewById(R.id.contact_details_layout2);
        mEmptyView = (TextView) detailView.findViewById(android.R.id.empty);

        // Set the contact name
        if (mIsTwoPaneLayout) {
            // If this is a two pane view, the following code changes the visibility of the contact
            // name in details. For a one-pane view, the contact name is displayed as a title.
            mContactName = (TextView) detailView.findViewById(R.id.contact_name);
            mContactName.setVisibility(View.VISIBLE);
        }

        return detailView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // If not being created from a previous state
        if (savedInstanceState == null) {
            // Sets the argument extra as the currently displayed contact
            setContact(getArguments() != null ?
                    (Uri) getArguments().getParcelable(EXTRA_CONTACT_URI) : null);
        } else {
            // If being recreated from a saved state, sets the contact from the incoming
            // savedInstanceState Bundle
            setContact((Uri) savedInstanceState.getParcelable(EXTRA_CONTACT_URI));
        }
    }

    /**
     * When the Fragment is being saved in order to change activity state, save the
     * currently-selected contact.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Saves the contact Uri
        outState.putParcelable(EXTRA_CONTACT_URI, mContactUri);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // When "edit" menu option selected
            case R.id.menu_edit_contact:
                // Standard system edit contact intent
                Intent intent = new Intent(Intent.ACTION_EDIT, mContactUri);

                // Because of an issue in Android 4.0 (API level 14), clicking Done or Back in the
                // People app doesn't return the user to your app; instead, it displays the People
                // app's contact list. A workaround, introduced in Android 4.0.3 (API level 15) is
                // to set a special flag in the extended data for the Intent you send to the People
                // app. The issue is does not appear in versions prior to Android 4.0. You can use
                // the flag with any version of the People app; if the workaround isn't needed,
                // the flag is ignored.
                intent.putExtra("finishActivityOnSaveCompleted", true);

                // Start the edit activity
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // Inflates the options menu for this fragment
        inflater.inflate(R.menu.contact_detail_menu, menu);

        // Gets a handle to the "find" menu item
        mEditContactMenuItem = menu.findItem(R.id.menu_edit_contact);

        // If contactUri is null the edit menu item should be hidden, otherwise
        // it is visible.
        mEditContactMenuItem.setVisible(mContactUri != null);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            // Two main queries to load the required information
            case ContactDetailQuery.QUERY_ID:
                // This query loads main contact details, see
                // ContactDetailQuery for more information.
                return new CursorLoader(getActivity(), mContactUri,
                        ContactDetailQuery.PROJECTION,
                        null, null, null);
            case ContactPhoneNumberQuery.QUERY_ID:
                // This query loads contact phone number, see
                // ContactPhoneQuery for more information.
                final Uri uri1 = Uri.withAppendedPath(mContactUri, Contacts.Data.CONTENT_DIRECTORY);
                return new CursorLoader(getActivity(), uri1,
                        ContactPhoneNumberQuery.PROJECTION,
                        ContactPhoneNumberQuery.SELECTION,
                        null, null);
            case ContactAddressQuery.QUERY_ID:
                // This query loads contact address details, see
                // ContactAddressQuery for more information.
                final Uri uri2 = Uri.withAppendedPath(mContactUri, Contacts.Data.CONTENT_DIRECTORY);
                return new CursorLoader(getActivity(), uri2,
                        ContactAddressQuery.PROJECTION,
                        ContactAddressQuery.SELECTION,
                        null, null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        // If this fragment was cleared while the query was running
        // eg. from from a call like setContact(uri) then don't do
        // anything.
        if (mContactUri == null) {
            return;
        }

        switch (loader.getId()) {
            case ContactDetailQuery.QUERY_ID:
                // Moves to the first row in the Cursor
                if (data.moveToFirst()) {
                    // For the contact details query, fetches the contact display name.
                    // ContactDetailQuery.DISPLAY_NAME maps to the appropriate display
                    // name field based on OS version.
                    final String contactName = data.getString(ContactDetailQuery.DISPLAY_NAME);
                    if (mIsTwoPaneLayout && mContactName != null) {
                        // In the two pane layout, there is a dedicated TextView
                        // that holds the contact name.
                        mContactName.setText(contactName);
                    } else {
                        // In the single pane layout, sets the activity title
                        // to the contact name. On HC+ this will be set as
                        // the ActionBar title text.
                        getActivity().setTitle(contactName);
                    }

                    // Clears out the details layout first in case the details
                    // layout has addresses from a previous data load still
                    // added as children.
                    mSocialNetworksLayout.removeAllViews();

                    // Each LinearLayout has the same LayoutParams so this can
                    // be created once and used for each social network.
                    final LinearLayout.LayoutParams layoutParams =
                            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT);

                    // Builds the Facebook social network layout
                    final LinearLayout layoutFB = buildSocialNetworkLayout(SNType.Facebook, contactName);
                    // Adds the new layout to the details layout
                    mSocialNetworksLayout.addView(layoutFB, layoutParams);

                    // Builds the Twitter social network layout
                    final LinearLayout layoutTwitter = buildSocialNetworkLayout(SNType.Twitter, contactName);
                    // Adds the new layout to the details layout
                    mSocialNetworksLayout.addView(layoutTwitter, layoutParams);
                }
                break;
            case ContactPhoneNumberQuery.QUERY_ID:
                // This query loads the contact phone numbers. More than
                // one number is possible.

                // Each LinearLayout has the same LayoutParams so this can
                // be created once and used for each phone number.
                final LinearLayout.LayoutParams layoutParamsPhone =
                        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);

                // Clears out the details layout first in case the details
                // layout has addresses from a previous data load still
                // added as children.
                mPhoneNumbersLayout.removeAllViews();

                // Loops through all the rows in the Cursor
                if (data.moveToFirst()) {
                    do {
                        // Builds the phone number layout
                        final LinearLayout layout = buildPhoneNumberLayout(
                                data.getInt(ContactPhoneNumberQuery.TYPE),
                                data.getString(ContactPhoneNumberQuery.LABEL),
                                data.getString(ContactPhoneNumberQuery.NUMBER));
                        // Adds the new number layout to the details layout
                        mPhoneNumbersLayout.addView(layout, layoutParamsPhone);
                    } while (data.moveToNext());
                } else {
                    // If nothing found, adds an empty address layout
                    mPhoneNumbersLayout.addView(buildEmptyPhoneNumberLayout(), layoutParamsPhone);
                }
                break;
            case ContactAddressQuery.QUERY_ID:
                // This query loads the contact address details. More than
                // one contact address is possible, so move each one to a
                // LinearLayout in a Scrollview so multiple addresses can
                // be scrolled by the user.

                // Each LinearLayout has the same LayoutParams so this can
                // be created once and used for each address.
                final LinearLayout.LayoutParams layoutParamsAddress =
                        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);

                // Clears out the details layout first in case the details
                // layout has addresses from a previous data load still
                // added as children.
                mAddressesLayout.removeAllViews();

                // Loops through all the rows in the Cursor
                if (data.moveToFirst()) {
                    do {
                        // Builds the address layout
                        final LinearLayout layout = buildAddressLayout(
                                data.getInt(ContactAddressQuery.TYPE),
                                data.getString(ContactAddressQuery.LABEL),
                                data.getString(ContactAddressQuery.ADDRESS));
                        // Adds the new address layout to the details layout
                        mAddressesLayout.addView(layout, layoutParamsAddress);
                    } while (data.moveToNext());
                } else {
                    // If nothing found, adds an empty address layout
                    mAddressesLayout.addView(buildEmptyAddressLayout(), layoutParamsAddress);
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Nothing to do here. The Cursor does not need to be released as it was never directly
        // bound to anything (like an adapter).
    }

    private enum SNType {
        Facebook ("Synchronize contact image"),
        Twitter ("Synchronize contact image");

        private final String action;

        private SNType(String s) {
            action = s;
        }

        public String toString(){
            return action;
        }
    }

    /**
     * Builds a phone number LinearLayout based on information from the Contacts Provider.
     * Each phone number for the contact gets its own LinearLayout object; for example, if the contact
     * has three phone numbers, then 3 LinearLayouts are generated.
     *
     * @param snType From
     * @param contactName From
     * @return A LinearLayout to add to the contact details layout,
     *         populated with the provided details.
     */
    private LinearLayout buildSocialNetworkLayout(final SNType snType, final String contactName) {

        // Inflates the item layout
        final LinearLayout snLayout =
                (LinearLayout) LayoutInflater.from(getActivity()).inflate(
                        R.layout.contact_detail_item, mSocialNetworksLayout, false);

        // Gets handles to the view objects in the layout
        final TextView headerTextView =
                (TextView) snLayout.findViewById(R.id.contact_detail_header);
        final TextView itemTextView =
                (TextView) snLayout.findViewById(R.id.contact_detail_item);
        final ImageButton actionButton =
                (ImageButton) snLayout.findViewById(R.id.button_action);


        // Sets objects in the item layout
        headerTextView.setText(snType.name());
        itemTextView.setText(snType.toString());
        actionButton.setImageResource(R.drawable.ic_action_refresh);

        //Sets the social network button action
        switch (snType) {
            case Facebook:
                // Defines an onClickListener object for the action button
                actionButton.setOnClickListener(new View.OnClickListener() {
                    // Defines what to do when users click the action button
                    @Override
                    public void onClick(View view) {
                        //Start FacebookLoginActivity
                        Intent intent = new Intent(getActivity(), FacebookLoginActivity.class);
                        intent.setData(mContactUri);
                        startActivityForResult(intent, PICK_FACEBOOK_USER_REQUEST);
                    }
                });
                break;

            case Twitter:
                // Defines an onClickListener object for the action button
                actionButton.setOnClickListener(new View.OnClickListener() {
                    // Defines what to do when users click the action button
                    @Override
                    public void onClick(View view) {
                        //TODO;
                        Toast.makeText(getActivity(), snType.name() + " action is not implemented yet.", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
        }

        return snLayout;
    }


    /**
     * Builds an empty phone number layout that just shows that no addresses
     * were found for this contact.
     *
     * @return A LinearLayout to add to the contact details layout
     */
    private LinearLayout buildEmptyPhoneNumberLayout() {
        return buildPhoneNumberLayout(0, null, null);
    }

    /**
     * Builds a phone number LinearLayout based on information from the Contacts Provider.
     * Each phone number for the contact gets its own LinearLayout object; for example, if the contact
     * has three phone numbers, then 3 LinearLayouts are generated.
     *
     * @param numberType From
     * @param numberTypeLabel From
     * @param phoneNumber From
     * @return A LinearLayout to add to the contact details layout,
     *         populated with the provided details.
     */
    private LinearLayout buildPhoneNumberLayout(int numberType, String numberTypeLabel,
                                                final String phoneNumber) {

        // Inflates the item layout
        final LinearLayout phoneNumberLayout =
                (LinearLayout) LayoutInflater.from(getActivity()).inflate(
                        R.layout.contact_detail_item, mPhoneNumbersLayout, false);

        // Gets handles to the view objects in the layout
        final TextView headerTextView =
                (TextView) phoneNumberLayout.findViewById(R.id.contact_detail_header);
        final TextView phoneTextView =
                (TextView) phoneNumberLayout.findViewById(R.id.contact_detail_item);
        final ImageButton makeCallButton =
                (ImageButton) phoneNumberLayout.findViewById(R.id.button_action);

        // If there's no addresses for the contact, shows the empty view and message, and hides the
        // header and button.
        if (numberTypeLabel == null && numberType == 0) {
            headerTextView.setVisibility(View.GONE);
            makeCallButton.setVisibility(View.GONE);
            phoneTextView.setText(R.string.no_phone);
        } else {
            // Gets phone number label type
            CharSequence label =
                    ContactsContract.CommonDataKinds.Phone.getTypeLabel(getResources(), numberType, numberTypeLabel);

            // Sets objects in the item layout
            headerTextView.setText(label);
            phoneTextView.setText(phoneNumber);
            makeCallButton.setImageResource(R.drawable.ic_action_call);

            // Defines an onClickListener object for the action button
            makeCallButton.setOnClickListener(new View.OnClickListener() {
                // Defines what to do when users click the action button
                @Override
                public void onClick(View view) {
                    String uri = "tel:" + phoneNumber.trim() ;
                    final Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse(uri));

                    // A PackageManager instance is needed to verify that there's a default app
                    // that handles calls.
                    final PackageManager packageManager = getActivity().getPackageManager();

                    // Checks for an activity that can handle this intent. Preferred in this
                    // case over Intent.createChooser() as it will still let the user choose
                    // a default (or use a previously set default) for calls.
                    if (packageManager.resolveActivity(
                            callIntent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                        startActivity(callIntent);
                    } else {
                        // If no default is found, displays a message that no activity can handle
                        // the view button.
                        Toast.makeText(getActivity(),
                                R.string.no_intent_found, Toast.LENGTH_SHORT).show();
                    }
                }
            });

        }
        return phoneNumberLayout;
    }

    /**
     * Builds an empty address layout that just shows that no addresses
     * were found for this contact.
     *
     * @return A LinearLayout to add to the contact details layout
     */
    private LinearLayout buildEmptyAddressLayout() {
        return buildAddressLayout(0, null, null);
    }

    /**
     * Builds an address LinearLayout based on address information from the Contacts Provider.
     * Each address for the contact gets its own LinearLayout object; for example, if the contact
     * has three postal addresses, then 3 LinearLayouts are generated.
     *
     * @param addressType From
     * {@link android.provider.ContactsContract.CommonDataKinds.StructuredPostal#TYPE}
     * @param addressTypeLabel From
     * {@link android.provider.ContactsContract.CommonDataKinds.StructuredPostal#LABEL}
     * @param address From
     * {@link android.provider.ContactsContract.CommonDataKinds.StructuredPostal#FORMATTED_ADDRESS}
     * @return A LinearLayout to add to the contact details layout,
     *         populated with the provided address details.
     */
    private LinearLayout buildAddressLayout(int addressType, String addressTypeLabel,
                                            final String address) {

        // Inflates the item layout
        final LinearLayout addressLayout =
                (LinearLayout) LayoutInflater.from(getActivity()).inflate(
                        R.layout.contact_detail_item, mAddressesLayout, false);

        // Gets handles to the view objects in the layout
        final TextView headerTextView =
                (TextView) addressLayout.findViewById(R.id.contact_detail_header);
        final TextView addressTextView =
                (TextView) addressLayout.findViewById(R.id.contact_detail_item);
        final ImageButton viewAddressButton =
                (ImageButton) addressLayout.findViewById(R.id.button_action);

        // If there's no addresses for the contact, shows the empty view and message, and hides the
        // header and button.
        if (addressTypeLabel == null && addressType == 0) {
            headerTextView.setVisibility(View.GONE);
            viewAddressButton.setVisibility(View.GONE);
            addressTextView.setText(R.string.no_address);
        } else {
            // Gets postal address label type
            CharSequence label =
                    StructuredPostal.getTypeLabel(getResources(), addressType, addressTypeLabel);

            // Sets TextView objects in the layout
            headerTextView.setText(label);
            addressTextView.setText(address);

            // Defines an onClickListener object for the address button
            viewAddressButton.setOnClickListener(new View.OnClickListener() {
                // Defines what to do when users click the address button
                @Override
                public void onClick(View view) {

                    final Intent viewIntent =
                            new Intent(Intent.ACTION_VIEW, constructGeoUri(address));

                    // A PackageManager instance is needed to verify that there's a default app
                    // that handles ACTION_VIEW and a geo Uri.
                    final PackageManager packageManager = getActivity().getPackageManager();

                    // Checks for an activity that can handle this intent. Preferred in this
                    // case over Intent.createChooser() as it will still let the user choose
                    // a default (or use a previously set default) for geo Uris.
                    if (packageManager.resolveActivity(
                            viewIntent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                        startActivity(viewIntent);
                    } else {
                        // If no default is found, displays a message that no activity can handle
                        // the view button.
                        Toast.makeText(getActivity(),
                                R.string.no_intent_found, Toast.LENGTH_SHORT).show();
                    }
                }
            });

        }
        return addressLayout;
    }

    /**
     * Constructs a geo scheme Uri from a postal address.
     *
     * @param postalAddress A postal address.
     * @return the geo:// Uri for the postal address.
     */
    private Uri constructGeoUri(String postalAddress) {
        // Concatenates the geo:// prefix to the postal address. The postal address must be
        // converted to Uri format and encoded for special characters.
        return Uri.parse(GEO_URI_SCHEME_PREFIX + Uri.encode(postalAddress));
    }

    /**
     * Fetches the width or height of the screen in pixels, whichever is larger. This is used to
     * set a maximum size limit on the contact photo that is retrieved from the Contacts Provider.
     * This limit prevents the app from trying to decode and load an image that is much larger than
     * the available screen area.
     *
     * @return The largest screen dimension in pixels.
     */
    private int getLargestScreenDimension() {
        // Gets a DisplayMetrics object, which is used to retrieve the display's pixel height and
        // width
        final DisplayMetrics displayMetrics = new DisplayMetrics();

        // Retrieves a displayMetrics object for the device's default display
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        final int height = displayMetrics.heightPixels;
        final int width = displayMetrics.widthPixels;

        // Returns the larger of the two values
        return height > width ? height : width;
    }

    /**
     * Decodes and returns the contact's thumbnail image.
     * @param contactUri The Uri of the contact containing the image.
     * @param imageSize The desired target width and height of the output image in pixels.
     * @return If a thumbnail image exists for the contact, a Bitmap image, otherwise null.
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private Bitmap loadContactPhoto(Uri contactUri, int imageSize) {

        // Ensures the Fragment is still added to an activity. As this method is called in a
        // background thread, there's the possibility the Fragment is no longer attached and
        // added to an activity. If so, no need to spend resources loading the contact photo.
        if (!isAdded() || getActivity() == null) {
            return null;
        }

        // Instantiates a ContentResolver for retrieving the Uri of the image
        final ContentResolver contentResolver = getActivity().getContentResolver();

        // Instantiates an AssetFileDescriptor. Given a content Uri pointing to an image file, the
        // ContentResolver can return an AssetFileDescriptor for the file.
        AssetFileDescriptor afd = null;

        if (Utils.hasICS()) {
            // On platforms running Android 4.0 (API version 14) and later, a high resolution image
            // is available from Photo.DISPLAY_PHOTO.
            try {
                // Constructs the content Uri for the image
                Uri displayImageUri = Uri.withAppendedPath(contactUri, Photo.DISPLAY_PHOTO);

                // Retrieves an AssetFileDescriptor from the Contacts Provider, using the
                // constructed Uri
                afd = contentResolver.openAssetFileDescriptor(displayImageUri, "r");
                // If the file exists
                if (afd != null) {
                    // Reads and decodes the file to a Bitmap and scales it to the desired size
                    return ImageLoader.decodeSampledBitmapFromDescriptor(
                            afd.getFileDescriptor(), imageSize, imageSize);
                }
            } catch (FileNotFoundException e) {
                // Catches file not found exceptions
                if (BuildConfig.DEBUG) {
                    // Log debug message, this is not an error message as this exception is thrown
                    // when a contact is legitimately missing a contact photo (which will be quite
                    // frequently in a long contacts list).
                    Log.d(TAG, "Contact photo not found for contact " + contactUri.toString()
                            + ": " + e.toString());
                }
            } finally {
                // Once the decode is complete, this closes the file. You must do this each time
                // you access an AssetFileDescriptor; otherwise, every image load you do will open
                // a new descriptor.
                if (afd != null) {
                    try {
                        afd.close();
                    } catch (IOException e) {
                        // Closing a file descriptor might cause an IOException if the file is
                        // already closed. Nothing extra is needed to handle this.
                    }
                }
            }
        }

        // If the platform version is less than Android 4.0 (API Level 14), use the only available
        // image URI, which points to a normal-sized image.
        try {
            // Constructs the image Uri from the contact Uri and the directory twig from the
            // Contacts.Photo table
            Uri imageUri = Uri.withAppendedPath(contactUri, Photo.CONTENT_DIRECTORY);

            // Retrieves an AssetFileDescriptor from the Contacts Provider, using the constructed
            // Uri
            afd = getActivity().getContentResolver().openAssetFileDescriptor(imageUri, "r");

            // If the file exists
            if (afd != null) {
                // Reads the image from the file, decodes it, and scales it to the available screen
                // area
                return ImageLoader.decodeSampledBitmapFromDescriptor(
                        afd.getFileDescriptor(), imageSize, imageSize);
            }
        } catch (FileNotFoundException e) {
            // Catches file not found exceptions
            if (BuildConfig.DEBUG) {
                // Log debug message, this is not an error message as this exception is thrown
                // when a contact is legitimately missing a contact photo (which will be quite
                // frequently in a long contacts list).
                Log.d(TAG, "Contact photo not found for contact " + contactUri.toString()
                        + ": " + e.toString());
            }
        } finally {
            // Once the decode is complete, this closes the file. You must do this each time you
            // access an AssetFileDescriptor; otherwise, every image load you do will open a new
            // descriptor.
            if (afd != null) {
                try {
                    afd.close();
                } catch (IOException e) {
                    // Closing a file descriptor might cause an IOException if the file is
                    // already closed. Ignore this.
                }
            }
        }

        // If none of the case selectors match, returns null.
        return null;
    }

    /**
     * This interface defines constants used by contact retrieval queries.
     */
    public interface ContactDetailQuery {
        // A unique query ID to distinguish queries being run by the
        // LoaderManager.
        final static int QUERY_ID = 1;

        // The query projection (columns to fetch from the provider)
        @SuppressLint("InlinedApi")
        final static String[] PROJECTION = {
                Contacts._ID,
                Utils.hasHoneycomb() ? Contacts.DISPLAY_NAME_PRIMARY : Contacts.DISPLAY_NAME,
        };

        // The query column numbers which map to each value in the projection
        final static int ID = 0;
        final static int DISPLAY_NAME = 1;
    }

    /**
     * This interface defines constants used by phone number retrieval queries.
     */
    public interface ContactPhoneNumberQuery {
        // A unique query ID to distinguish queries being run by the
        // LoaderManager.
        final static int QUERY_ID = 2;

        // The query projection (columns to fetch from the provider)
        final static String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Phone._ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.LABEL
        };

        // The query selection criteria. In this case matching against the
        // StructuredPostal content mime type.
        final static String SELECTION =
                Data.MIMETYPE + "='" +  ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'";

        // The query column numbers which map to each value in the projection
        final static int ID = 0;
        final static int NUMBER = 1;
        final static int TYPE = 2;
        final static int LABEL = 3;
    }

    /**
     * This interface defines constants used by address retrieval queries.
     */
    public interface ContactAddressQuery {
        // A unique query ID to distinguish queries being run by the
        // LoaderManager.
        final static int QUERY_ID = 3;

        // The query projection (columns to fetch from the provider)
        final static String[] PROJECTION = {
                StructuredPostal._ID,
                StructuredPostal.FORMATTED_ADDRESS,
                StructuredPostal.TYPE,
                StructuredPostal.LABEL,
        };

        // The query selection criteria. In this case matching against the
        // StructuredPostal content mime type.
        final static String SELECTION =
                Data.MIMETYPE + "='" + StructuredPostal.CONTENT_ITEM_TYPE + "'";

        // The query column numbers which map to each value in the projection
        final static int ID = 0;
        final static int ADDRESS = 1;
        final static int TYPE = 2;
        final static int LABEL = 3;
    }
}