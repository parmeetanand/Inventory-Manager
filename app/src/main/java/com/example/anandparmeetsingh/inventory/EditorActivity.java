package com.example.anandparmeetsingh.inventory;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.support.v4.app.NavUtils;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.anandparmeetsingh.inventory.data.InventoryContract;
import com.example.anandparmeetsingh.inventory.data.InventoryDbHelper;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;

public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int IMAGE_REQUEST = 0;
    private static final String FILE_PROVIDER_AUTHORITY = "com.example.android.inventoryApp";
    private static final int EXISTING_INVENTORY_LOADER = 0;
    InventoryDbHelper mDbHelper;
    InventoryCursorAdapter mCursorLoader;
    private EditText mNameEditText;
    private EditText mDetailText;
    private EditText mStockEditText;
    private EditText mPriceText;
    private boolean mInventoryHasChanged = false;
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mInventoryHasChanged = true;
            return false;
        }
    };
    private ImageView mImageView;
    private Bitmap mBitmap;
    private boolean galleryPic = false;
    private Uri mUri;
    private String uriString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);
        Button buttonChoose = (Button) findViewById(R.id.add_button);
        mImageView = (ImageView) findViewById(R.id.add_image);
        Intent intent = getIntent();

        mUri = intent.getData();

        //check if  intent that contain the Uri or not
        if (mUri == null) {
            setTitle(getString(R.string.editor_activity_title_new_pet));

            //invalid option menu, so the "delete" menu option can be hidden
            invalidateOptionsMenu();
        } else {
            setTitle(getString(R.string.editor_activity_title_editor_pet));

            getLoaderManager().initLoader(EXISTING_INVENTORY_LOADER, null, this);
        }

        mNameEditText = (EditText) findViewById(R.id.edit_name);
        mDetailText = (EditText) findViewById(R.id.edit_detail);
        mPriceText = (EditText) findViewById(R.id.edit_price);
        mStockEditText = (EditText) findViewById(R.id.edit_stock);

        //setup OnTouchListener on all input fields to know which has unsaved change
        mNameEditText.setOnTouchListener(mTouchListener);
        mDetailText.setOnTouchListener(mTouchListener);
        mPriceText.setOnTouchListener(mTouchListener);
        mStockEditText.setOnTouchListener(mTouchListener);
        buttonChoose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                image();
            }
        });

        Button order = (Button) findViewById(R.id.add_quantity_button);
        order.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("*/*");
                startActivity(Intent.createChooser(intent, "Send Email"));
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                save();
                return true;
            case R.id.action_delete:

                showDeleteDialog();
                return true;
            case android.R.id.home:
                if (!hasChanged()) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                showUnsavedChanges(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void save() {
        if (checkValidity()) {
            mDbHelper = new InventoryDbHelper(this);
            EditText name = (EditText) findViewById(R.id.edit_name);
            EditText price = (EditText) findViewById(R.id.edit_price);
            EditText detail = (EditText) findViewById(R.id.edit_detail);
            EditText quantity = (EditText) findViewById(R.id.edit_stock);

            String names = name.getText().toString();
            int prices = Integer.parseInt(price.getText().toString());
            String details = detail.getText().toString();
            int quant = Integer.parseInt(quantity.getText().toString());
            ContentValues values = new ContentValues();
            values.put(InventoryContract.InventoryEntry.COLUMN_INVENTORY_NAME, names);
            values.put(InventoryContract.InventoryEntry.COLUMN_INVENTORY_PRICE, prices);
            values.put(InventoryContract.InventoryEntry.COLUMN_INVENTORY_QUANTITY, quant);
            values.put(InventoryContract.InventoryEntry.COLUMN_INVENTORY_DETAIL, details);
            values.put(InventoryContract.InventoryEntry.COLUMN_IMAGE, uriString);

            Uri newUri = getContentResolver().insert(InventoryContract.InventoryEntry.CONTENT_URI, values);

            if (newUri == null) {
                Toast.makeText(this, getString(R.string.detail_insert_inventory_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.detail_insert_inventory_successful),
                        Toast.LENGTH_SHORT).show();
            }
            finish();
        } else {
            Toast.makeText(this, getString(R.string.editor_fields_blank),
                    Toast.LENGTH_SHORT).show();

        }
    }


    private void showUnsavedChanges(
            DialogInterface.OnClickListener discardButtonClickListener) {
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        android.support.v7.app.AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onBackPressed() {
        if (!hasChanged()) {
            super.onBackPressed();
            return;
        }
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                };

        showUnsavedChanges(discardButtonClickListener);
    }

    public boolean checkValidity() {
        EditText Name = (EditText) findViewById(R.id.edit_name);
        EditText Price = (EditText) findViewById(R.id.edit_price);
        EditText Quantity = (EditText) findViewById(R.id.edit_stock);
        EditText Detail = (EditText) findViewById(R.id.edit_detail);
        return (Name.getText().toString().trim().length() != 0 &&
                Price.getText().toString().trim().length() != 0 &&
                Quantity.getText().toString().trim().length() != 0 &&
                Detail.getText().toString().trim().length() != 0 &&
                uriString != null);
    }

    private boolean hasChanged() {
        EditText Name = (EditText) findViewById(R.id.edit_name);
        EditText Price = (EditText) findViewById(R.id.edit_price);
        EditText Quantity = (EditText) findViewById(R.id.edit_stock);
        EditText Detail = (EditText) findViewById(R.id.edit_detail);
        return  Name.getText().toString().trim().length() != 0 ||
                Price.getText().toString().trim().length() != 0 ||
                Quantity.getText().toString().trim().length() != 0 ||
                Detail.getText().toString().trim().length() != 0 ||
                uriString != null;
    }

    public void image() {
        Intent intent;

        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }

        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), IMAGE_REQUEST);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCodes, Intent resultData) {

        if (resultCodes == Activity.RESULT_OK) {

            if (resultData != null) {
                mUri = resultData.getData();

                mBitmap = getBitmapFromUri(mUri);
                mImageView.setImageBitmap(mBitmap);
                uriString = getShareableImageUri().toString();
                galleryPic = true;
            }
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) {
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = null;
            if (parcelFileDescriptor != null) {
                fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            }
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            if (parcelFileDescriptor != null) {
                parcelFileDescriptor.close();
            }
            return image;
        } catch (Exception e) {
            return null;
        } finally {
            try {
                if (parcelFileDescriptor != null) {
                    parcelFileDescriptor.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Uri getShareableImageUri() {
        Uri imagesUri;

        if (galleryPic) {
            String filename = PathFinder();
            savingInFile(getCacheDir(), filename, mBitmap, Bitmap.CompressFormat.JPEG, 100);
            File imagesFile = new File(getCacheDir(), filename);

            imagesUri = FileProvider.getUriForFile(
                    this, FILE_PROVIDER_AUTHORITY, imagesFile);

        } else {
            imagesUri = mUri;
        }

        return imagesUri;
    }

    public String PathFinder() {
        Cursor returnCursor =
                getContentResolver().query(mUri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);

        if (returnCursor != null) {
            returnCursor.moveToFirst();
        }
        String fileNames = null;
        if (returnCursor != null) {
            fileNames = returnCursor.getString(returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
        }
        if (returnCursor != null) {
            returnCursor.close();
        }
        return fileNames;
    }


    public boolean savingInFile(File dir, String fileName, Bitmap bm,
                                Bitmap.CompressFormat format, int quality) {
        File imagesFile = new File(dir, fileName);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(imagesFile);
            bm.compress(format, quality, fos);
            fos.close();

            return true;
        } catch (IOException e) {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return false;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        //Define the projection that specifies the column that we need
        String[] projection = {
                InventoryContract.InventoryEntry._ID,
                InventoryContract.InventoryEntry.COLUMN_INVENTORY_NAME,
                InventoryContract.InventoryEntry.COLUMN_INVENTORY_DETAIL,
                InventoryContract.InventoryEntry.COLUMN_INVENTORY_PRICE,
                InventoryContract.InventoryEntry.COLUMN_INVENTORY_QUANTITY
        };

        return new CursorLoader(this, mUri,
                projection,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        //Update new Cursor that contain the updated data
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        //proceed with moving to the first row of the cursor and reading data from it
        if (cursor.moveToFirst()) {
            int nameColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_INVENTORY_NAME);
            int breedColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_INVENTORY_DETAIL);
            int genderColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_INVENTORY_QUANTITY);
            int weightColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_INVENTORY_PRICE);

            //extract the value of the cursor from given index
            String name = cursor.getString(nameColumnIndex);
            String breed = cursor.getString(breedColumnIndex);
            int gender = cursor.getInt(genderColumnIndex);
            int weight = cursor.getInt(weightColumnIndex);

            //update the views on the screen with the value from the database
            mNameEditText.setText(name);
            mDetailText.setText(breed);
            mPriceText.setText(Integer.toString(weight));
            mStockEditText.setText(Integer.toString(gender));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        //callback called when the data need to be deleted
        mNameEditText.setText("");
        mDetailText.setText("");
        mPriceText.setText("");
        mStockEditText.setText("");
        getContentResolver().notifyChange(InventoryContract.InventoryEntry.CONTENT_URI, null);
    }


    private void showDeleteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //user click the Delete button
                deleteInventory();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //User click cancel
                if (dialogInterface != null) {
                    dialogInterface.dismiss();
                }
            }
        });

        //create and show the dialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void deleteInventory() {
        if (mUri != null) {
            //pass in null for the selection and selectionArgs
            int rowsDeleted = getContentResolver().delete(mUri, null, null);

            //show toast whether the outcast
            if (rowsDeleted == 0) {
                //no row mean nothing to delete show error
                Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show();
            } else {
                //otherwise has row to delete show successful
                Toast.makeText(this, "Delete Successful", Toast.LENGTH_SHORT).show();
            }
        }
        getContentResolver().notifyChange(InventoryContract.InventoryEntry.CONTENT_URI, null);

        //close activity
        finish();
    }
}