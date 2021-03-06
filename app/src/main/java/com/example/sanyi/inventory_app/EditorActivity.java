package com.example.sanyi.inventory_app;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.sanyi.inventory_app.data.StoreContract.StoreEntry;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    //Code to decide which action to be taken
    Integer REQUEST_CAMERA = 1, SELECT_FILE = 0;
    private static final int EXISTING_ITEM_LOADER = 0;
    ImageView productImage;
    ImageButton reorder;
    Bitmap photoToSet;
    String pathToPicture = "";
    Boolean needChanges = false;
    EditText phoneNumber;
    EditText webadress;
    EditText itemName;
    Spinner supplierSpinner;
    EditText price;
    EditText quantity;
    ImageButton minusButton;
    ImageButton plusButton;

    //Declaring which permissions needed
    String[] permissionsCamera = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.MEDIA_CONTENT_CONTROL};

    String[] permissionsGalery = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.MEDIA_CONTENT_CONTROL};
    private Uri CurrentItemUri;
    private View.OnClickListener changequantity = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.minusId:
                    changeQuantityValue(-1);
                    break;
                case R.id.plusId:
                    changeQuantityValue(1);
                    break;
            }
        }
    };

    private void changeQuantityValue(int i) {
        int currentValue = Integer.parseInt(quantity.getText().toString());
        currentValue += i;
        if (currentValue > 0) {
            quantity.setText(String.valueOf(currentValue));
        }
    }

    private int mSupplier;

    private View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            needChanges = true;
            return false;
        }
    };

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // Insert mode is on
        if (CurrentItemUri == null) {
            MenuItem menuItem = menu.findItem(R.id.delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete:
                showDeleteConfirmationDialog();
                break;
            case R.id.save:
                saveItem();
                if (!needChanges) {
                    finish();
                }
                return true;
            case android.R.id.home:
                if (!needChanges) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }
                DialogInterface.OnClickListener discardButtonClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    }
                };
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.deleteItem));
        // User wants to delete that item from the list
        builder.setPositiveButton(getString(R.string.deleteYes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteItem();
            }
        });
        // User want to keep that record
        builder.setNegativeButton(getString(R.string.deleteNo), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        // Showing the Alert Dialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
    // Deleting the item from the database
    private void deleteItem() {
        if (CurrentItemUri != null) {
            int rowsDeleted = getContentResolver().delete(CurrentItemUri, null, null);

            if (rowsDeleted == 0) {
                Toast.makeText(this, getString(R.string.error_delete), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.succesfull_delete), Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }

    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.alerDialogTitle));
        // User selets the discard option
        builder.setPositiveButton(getString(R.string.plusAnswer), discardButtonClickListener);
        // User selects the keep edition option
        builder.setNegativeButton(getString(R.string.negativeAnswer), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);
        Log.e("Value of location: ", pathToPicture);
        Intent intent = getIntent();
        CurrentItemUri = intent.getData();
        if (CurrentItemUri == null) {
            setTitle(getString(R.string.addItem));

        } else {
            setTitle(getString(R.string.editItem));
            // Display the loader data
            getSupportLoaderManager().initLoader(EXISTING_ITEM_LOADER, null, this);
        }
        // Setting up views
        productImage = (ImageView) findViewById(R.id.itemPictureId);
        itemName = (EditText) findViewById(R.id.NameEditText);
        price = (EditText) findViewById(R.id.PriceEditText);
        supplierSpinner = (Spinner) findViewById(R.id.SupplierSpinner);
        phoneNumber = (EditText) findViewById(R.id.PhoneEditText);
        webadress = (EditText) findViewById(R.id.URLEditText);
        quantity = (EditText) findViewById(R.id.quantityEditText);
        minusButton = (ImageButton) findViewById(R.id.minusId);
        plusButton = (ImageButton) findViewById(R.id.plusId);
        reorder = (ImageButton) findViewById(R.id.callOrder);

        // Setting up ontouch listeners
        productImage.setOnTouchListener(touchListener);
        itemName.setOnTouchListener(touchListener);
        price.setOnTouchListener(touchListener);
        supplierSpinner.setOnTouchListener(touchListener);
        phoneNumber.setOnTouchListener(touchListener);
        webadress.setOnTouchListener(touchListener);
        quantity.setOnTouchListener(touchListener);
        minusButton.setOnTouchListener(touchListener);
        plusButton.setOnTouchListener(touchListener);

        productImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SelectImage();
            }
        });
        minusButton.setOnClickListener(changequantity);
        plusButton.setOnClickListener(changequantity);
        setupSpinner();
    }

    //Setting up spinner items
    private void setupSpinner() {

        ArrayAdapter supplierSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_supplier_options, android.R.layout.simple_spinner_dropdown_item);
        supplierSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        supplierSpinner.setAdapter(supplierSpinnerAdapter);
        supplierSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSupplier = parent.getSelectedItemPosition();
                Log.e("SupplerID", String.valueOf(mSupplier));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mSupplier = 0;
            }
        });
    }

    // Saving the data to the SQLite database
    private void saveItem() {
        String nameString = itemName.getText().toString().trim();
        String priceString = price.getText().toString().trim();
        int supplerId = mSupplier;
        String phoneString = phoneNumber.getText().toString().trim();
        String urlString = webadress.getText().toString().trim();
        String quantityString = quantity.getText().toString();
        if (TextUtils.isEmpty(quantityString) || !StoreEntry.isValidnumber(Integer.parseInt(quantityString))) {
            Toast.makeText(this, getString(R.string.quantity_empty), Toast.LENGTH_SHORT).show();
        }

        if (CurrentItemUri == null && TextUtils.isEmpty(nameString) && TextUtils.isEmpty(priceString)
                && TextUtils.isEmpty(String.valueOf(supplerId)) && TextUtils.isEmpty(phoneString) && TextUtils.isEmpty(urlString)
                && TextUtils.isEmpty(quantityString)) {
            return;
        }
        if (!TextUtils.isEmpty(nameString) && !TextUtils.isEmpty(priceString)
                && !TextUtils.isEmpty(String.valueOf(supplerId)) && !TextUtils.isEmpty(phoneString) && !TextUtils.isEmpty(urlString)
                && !TextUtils.isEmpty(quantityString) && StoreEntry.isValidnumber(Integer.parseInt(quantityString))) {
            needChanges = false;
            ContentValues values = new ContentValues();
            values.put(StoreEntry.COLUMN_PICTURE_PATH, pathToPicture);
            values.put(StoreEntry.COLUMN_ITEM_NAME, nameString);
            values.put(StoreEntry.COLUMN_PRICE, priceString);
            values.put(StoreEntry.COLUMN_ITEM_NUMBER, Integer.parseInt(quantityString));
            values.put(StoreEntry.COLUMN_SUPPLIER, supplerId);
            values.put(StoreEntry.COLUMN_PHONE, phoneString);
            values.put(StoreEntry.COLUMN_URL, urlString);
            // Coming from mainActivity to insert a new item
            if (CurrentItemUri == null) {
                Uri uri = getContentResolver().insert(StoreEntry.CONTENT_URI, values);

                if (uri == null) {
                    Toast.makeText(this, getString(R.string.error_saving), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, getString(R.string.succesfull_saving), Toast.LENGTH_SHORT).show();
                }
            }
            // Coming from MainActivity to edit some item
            else {
                int rowsAffected = getContentResolver().update(CurrentItemUri, values, null, null);

                if (rowsAffected == 0) {
                    Toast.makeText(this, getString(R.string.error_updating), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, getString(R.string.succesfull_update), Toast.LENGTH_SHORT).show();
                }
            }
        } else if (TextUtils.isEmpty(nameString)) {
            needChanges = true;
            Toast.makeText(this, getString(R.string.name_empty), Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(priceString) || !StoreEntry.isValidnumber(Integer.parseInt(priceString))) {
            needChanges = true;
            Toast.makeText(this, getString(R.string.price_empty), Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(phoneString)) {
            needChanges = true;
            Toast.makeText(this, getString(R.string.phone_empty), Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(urlString)) {
            needChanges = true;
            Toast.makeText(this, getString(R.string.url_empty), Toast.LENGTH_SHORT).show();
        }
    }


    // Image selector function
    private void SelectImage() {

        final CharSequence[] items = {"Camera", "Gallery", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(EditorActivity.this);
        builder.setTitle("Add Image");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                if (items[i].equals("Camera")) {
                    // Checking user has permissions for everything
                    if (ActivityCompat.checkSelfPermission(EditorActivity.this, permissionsCamera[0]) != PackageManager.PERMISSION_GRANTED
                            || ActivityCompat.checkSelfPermission(EditorActivity.this, permissionsCamera[1]) != PackageManager.PERMISSION_GRANTED
                            || ActivityCompat.checkSelfPermission(EditorActivity.this, permissionsCamera[2]) != PackageManager.PERMISSION_GRANTED) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(EditorActivity.this, permissionsCamera[0])
                                || ActivityCompat.shouldShowRequestPermissionRationale(EditorActivity.this, permissionsCamera[1])
                                || ActivityCompat.shouldShowRequestPermissionRationale(EditorActivity.this, permissionsCamera[2])) {
                            //Show Information about why you need the permission
                            AlertDialog.Builder builder = new AlertDialog.Builder(EditorActivity.this);
                            builder.setTitle("Need Multiple Permissions");
                            builder.setMessage("This app needs Camera and writing permissions.");
                            builder.setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                    ActivityCompat.requestPermissions(EditorActivity.this, permissionsCamera, MULTIPLE_REQUEST_PHOTO);
                                }
                            });
                            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                            builder.show();
                        } else {
                            ActivityCompat.requestPermissions(EditorActivity.this, permissionsCamera, MULTIPLE_REQUEST_PHOTO);
                        }
                    } else {
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(intent, REQUEST_CAMERA);
                    }
                } else if (items[i].equals("Gallery")) {
                    // User has permissions for everything
                    if (ActivityCompat.checkSelfPermission(EditorActivity.this, permissionsGalery[0]) != PackageManager.PERMISSION_GRANTED
                            || ActivityCompat.checkSelfPermission(EditorActivity.this, permissionsGalery[1]) != PackageManager.PERMISSION_GRANTED
                            || ActivityCompat.checkSelfPermission(EditorActivity.this, permissionsGalery[2]) != PackageManager.PERMISSION_GRANTED) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(EditorActivity.this, permissionsGalery[0])
                                || ActivityCompat.shouldShowRequestPermissionRationale(EditorActivity.this, permissionsGalery[1])
                                || ActivityCompat.shouldShowRequestPermissionRationale(EditorActivity.this, permissionsGalery[2])) {
                            //Show Information about why you need the permission
                            AlertDialog.Builder builder = new AlertDialog.Builder(EditorActivity.this);
                            builder.setTitle("Need Multiple Permissions");
                            builder.setMessage("This app needs read and writing permissions.");
                            builder.setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                    ActivityCompat.requestPermissions(EditorActivity.this, permissionsGalery, MULTIPLE_REQUEST_GALLERY);
                                }
                            });
                            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                            builder.show();
                        } else {
                            ActivityCompat.requestPermissions(EditorActivity.this, permissionsGalery, MULTIPLE_REQUEST_GALLERY);
                        }
                    } else {
                        Intent intent = new Intent(Intent.ACTION_PICK);
                        intent.setType("image/*");
                        startActivityForResult(intent, SELECT_FILE);
                    }
                } else if (items[i].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    // Overriding the activity result function to get the picture from the gallery what the user has selected
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CAMERA) {
                photoToSet = (Bitmap) data.getExtras().get("data");
                productImage.setImageBitmap(photoToSet);
                saveImage(photoToSet);
                Toast.makeText(EditorActivity.this, "Image Saved", Toast.LENGTH_SHORT).show();
            } else if (requestCode == SELECT_FILE) {
                Uri selected = data.getData();
                InputStream imagestream = null;
                try {
                    imagestream = getContentResolver().openInputStream(selected);
                } catch (FileNotFoundException e) {
                    Log.e("File", "Not found");
                }
                photoToSet = BitmapFactory.decodeStream(imagestream);
                productImage.setImageURI(selected);
                saveImage(photoToSet);
            }
        }
    }

    public static final String IMAGE_DIRECTORY = "/items";

    // Saving imvage the user took right now to the gallery
    private String saveImage(Bitmap photoToSet) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        photoToSet.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
        File wallpaperDirectory = new File(Environment.getExternalStorageDirectory() + IMAGE_DIRECTORY);
        // creating file if needed
        if (!wallpaperDirectory.exists()) {
            wallpaperDirectory.mkdirs();
        }
        try {
            File f = new File(wallpaperDirectory, Calendar.getInstance().getTimeInMillis() + ".jpg");
            f.createNewFile();
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
            MediaScannerConnection.scanFile(this, new String[]{f.getPath()},
                    new String[]{"image/jpeg"}, null);
            fo.close();
            Log.e("TAG", "FILE_SAVEd" + f.getAbsolutePath());
            pathToPicture = f.getAbsolutePath();

        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return "";
    }

    // Initalizing loader
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                StoreEntry._ID,
                StoreEntry.COLUMN_PICTURE_PATH,
                StoreEntry.COLUMN_ITEM_NAME,
                StoreEntry.COLUMN_PRICE,
                StoreEntry.COLUMN_SUPPLIER,
                StoreEntry.COLUMN_PHONE,
                StoreEntry.COLUMN_URL,
                StoreEntry.COLUMN_ITEM_NUMBER};

        return new CursorLoader(this,
                CurrentItemUri,
                projection,
                null,
                null,
                null);
    }

    @Override
    public void onBackPressed() {
        if (!needChanges) {
            super.onBackPressed();
            return;
        }
        DialogInterface.OnClickListener discardButtonClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        };
        // Show dialog that not everything is saved
        showUnsavedChangesDialog(discardButtonClickListener);

    }

    final int REQEST_DIAL = 100;
    final int MULTIPLE_REQUEST_GALLERY = 101;
    final int MULTIPLE_REQUEST_PHOTO = 102;


    Intent callReorder;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQEST_DIAL:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startActivity(callReorder);
                } else {
                    Toast.makeText(EditorActivity.this, "You dont have permission", Toast.LENGTH_SHORT).show();
                }
                break;
            case MULTIPLE_REQUEST_PHOTO:
                if (ActivityCompat.checkSelfPermission(EditorActivity.this, permissionsCamera[0]) == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent, REQUEST_CAMERA);
                }
                break;
            // REquesting permissions for using galery
            case MULTIPLE_REQUEST_GALLERY:
                if (ActivityCompat.checkSelfPermission(EditorActivity.this, permissionsGalery[0]) == PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(EditorActivity.this, permissionsGalery[1]) == PackageManager.PERMISSION_GRANTED) {

                    Intent intent = new Intent(Intent.ACTION_PICK);
                    intent.setType("image/*");
                    startActivityForResult(intent, SELECT_FILE);
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data == null || data.getCount() < 1) {
            return;
        }
        if (data.moveToFirst()) {
            // Loading content to the screen
            int nameIndex = data.getColumnIndex(StoreEntry.COLUMN_ITEM_NAME);
            int imgpathindex = data.getColumnIndex(StoreEntry.COLUMN_PICTURE_PATH);
            int priceIndex = data.getColumnIndex(StoreEntry.COLUMN_PRICE);
            int supplierIndex = data.getColumnIndex(StoreEntry.COLUMN_SUPPLIER);
            int phoneIndex = data.getColumnIndex(StoreEntry.COLUMN_PHONE);
            int urlIndex = data.getColumnIndex(StoreEntry.COLUMN_URL);
            int quantityIndex = data.getColumnIndex(StoreEntry.COLUMN_ITEM_NUMBER);

            String name = data.getString(nameIndex);
            String imagePath = data.getString(imgpathindex);
            int priceint = data.getInt(priceIndex);
            int supplier = data.getInt(supplierIndex);
            final String phone = data.getString(phoneIndex);
            String url = data.getString(urlIndex);
            int quantityint = data.getInt(quantityIndex);

            Uri uri = Uri.parse(imagePath);
            productImage.setImageURI(uri);
            itemName.setText(name);
            price.setText(String.valueOf(priceint));
            supplierSpinner.setSelection(supplier);
            phoneNumber.setText(phone);
            webadress.setText(url);
            quantity.setText(String.valueOf(quantityint));
            reorder.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    callReorder = new Intent(Intent.ACTION_CALL);
                    callReorder.setData(Uri.parse("tel:" + phone));
                    int hasDialPermission = ContextCompat.checkSelfPermission(EditorActivity.this, Manifest.permission.CALL_PHONE);
                    if (hasDialPermission != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(EditorActivity.this, new String[]{Manifest.permission.CALL_PHONE}, REQEST_DIAL);
                        return;
                    } else {
                        startActivity(callReorder);
                    }

                }
            });
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        itemName.setText("");
        price.setText("");
        supplierSpinner.setSelection(0);
        phoneNumber.setText("");
        webadress.setText("");
        quantity.setText("");
    }
}
