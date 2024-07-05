package com.particlesdevs.photoncamera.gallery.files;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.IntentSender;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.particlesdevs.photoncamera.gallery.interfaces.ImagesDeletedCallback;
import com.particlesdevs.photoncamera.settings.PreferenceKeys;

import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by Vibhor Srivastava on October 13, 2021
 */
public class GalleryFileOperations {
    public static final int REQUEST_PERM_DELETE = 1010;
    private static final String[] INCLUDED_IMAGE_FOLDERS = new String[]{"%DCIM/PhotonCamera/%", "%DCIM/PhotonCamera/Raw/%", "%DCIM/Camera/%"};
    private static final ArrayList<String> SELECTED_FOLDERS_IDS = new ArrayList<>();
    private static final ArrayList<ImagesFolder> ALL_FOLDERS = new ArrayList<>();
    private static final ArrayList<ImagesFolder> SELECTED_FOLDERS = new ArrayList<>();

    public static ArrayList<ImagesFolder> getSelectedFolders() {
        return SELECTED_FOLDERS;
    }

    public static ArrayList<ImagesFolder> _fetchSelectedFolders(ContentResolver contentResolver) {
        FindAllFoldersWithImages(contentResolver);
        SELECTED_FOLDERS_IDS.clear();
        SELECTED_FOLDERS.clear();
        SELECTED_FOLDERS_IDS.addAll(PreferenceKeys.getStringSet(PreferenceKeys.Key.FOLDERS_LIST));
        if (SELECTED_FOLDERS_IDS.isEmpty()) {
            SELECTED_FOLDERS_IDS.add("Camera"); //in case the user has not selected any folder
            SELECTED_FOLDERS_IDS.add("Raw");
        }
        SELECTED_FOLDERS_IDS.forEach(s -> ALL_FOLDERS.forEach(imagesFolder -> {
            if (String.valueOf(imagesFolder.folderId).equals(s) || imagesFolder.folderName.equals(s)) {
                SELECTED_FOLDERS.add(imagesFolder);
            }
        }));
        SELECTED_FOLDERS.sort(Comparator.comparing(o -> o.folderName));
        return SELECTED_FOLDERS;
    }

    public static List<ImageFile> extractAllSelectedImages() {
        ArrayList<ImageFile> imageFiles = new ArrayList<>();
        SELECTED_FOLDERS.forEach(imagesFolder -> imageFiles.addAll(imagesFolder.getAllImageFiles()));
/*
        ArrayList<ImageFile> images = new ArrayList<>();
        String[] projection = new String[]{MediaStore.MediaColumns.DATA,MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATE_ADDED, MediaStore.Images.Media.SIZE};

        String selectionColumn = Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q ? MediaStore.Images.Media.BUCKET_ID : MediaStore.Images.Media.DATA;
        String selection = "";
        for (int i = 0; i < selectedFolders.size(); i++) {
            if (i == selectedFolders.size() - 1)
                selection = selection.concat(selectionColumn).concat(" like ? ");
            else
                selection = selection.concat(selectionColumn).concat(" like ? OR ");
        }
        //String selection = selectionColumn + " like ? OR " + selectionColumn + " like ? OR " + selectionColumn + " like ?";
        String[] selectionArgs = selectedFolders.toArray(new String[]{});

        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";
        final Cursor cursor = contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder);

        if (cursor != null) {

            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED);
            int displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);
            int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);


            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumn);
                Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                String displayName = cursor.getString(displayNameColumn);
                long dateModified = TimeUnit.SECONDS.toMillis(cursor.getLong(dateModifiedColumn));
                long size = cursor.getLong(sizeColumn);
                String absolutePath=cursor.getString(dataColumn);

                ImageFile image = new ImageFile(id, contentUri, displayName, dateModified, size,absolutePath);
                images.add(image);
            }
            cursor.close();
        }
*/
        imageFiles.sort(Comparator.comparingLong(value -> -value.getLastModified()));
        return imageFiles;
    }

    @Nullable
    public static ImageFile fetchLatestImage(ContentResolver contentResolver) {
        ImageFile imageFile = null;

        String[] projection = new String[]{MediaStore.MediaColumns.DATA,MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATE_ADDED, MediaStore.Images.Media.SIZE};

        String selectionColumn = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? MediaStore.Images.Media.RELATIVE_PATH : MediaStore.Images.Media.DATA;
        String selection = selectionColumn + " like ? OR " + selectionColumn + " like ? OR " + selectionColumn + " like ?";
        String[] selectionArgs = INCLUDED_IMAGE_FOLDERS;

        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";

        final Cursor cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder);
        if (cursor != null) {
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED);
            int displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);
            int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);

            if (cursor.moveToFirst()) {
                long id = cursor.getLong(idColumn);
                Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                String displayName = cursor.getString(displayNameColumn);
                long dateModified = TimeUnit.SECONDS.toMillis(cursor.getLong(dateModifiedColumn));
                long size = cursor.getLong(sizeColumn);
                String absolutePath=cursor.getString(dataColumn);
                imageFile = new ImageFile(id, contentUri, displayName, dateModified, size,absolutePath);
            }
            cursor.close();
        }
        return imageFile;
    }

    public static Uri createNewImageFile(ContentResolver contentResolver, String relativePath, String newImageName) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, newImageName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, URLConnection.guessContentTypeFromName(newImageName));
        String column = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? MediaStore.MediaColumns.RELATIVE_PATH : MediaStore.MediaColumns.DATA;
        values.put(column, relativePath);
        return contentResolver.insert(MediaStore.Files.getContentUri("external"), values);
    }

    public static void deleteImageFiles(Activity activity, List<ImageFile> toDelete, ImagesDeletedCallback deletedCallback) {
        List<Uri> toDeleteUriList = toDelete.stream().map(ImageFile::getFileUri).collect(Collectors.toList());
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            PendingIntent pi = MediaStore.createDeleteRequest(activity.getContentResolver(), toDeleteUriList);
            try {
                ActivityCompat.startIntentSenderForResult(activity, pi.getIntentSender(), REQUEST_PERM_DELETE, null, 0, 0, 0, null);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
                deletedCallback.deleted(false);
            }
        } else {
            ContentResolver contentResolver = activity.getContentResolver();
            for (ImageFile file : toDelete) {
                try {
                    contentResolver.delete(file.getFileUri(), MediaStore.Images.Media._ID + "= ?", new String[]{String.valueOf(file.getId())});
                } catch (SecurityException e) {
                    e.printStackTrace();
                    deletedCallback.deleted(false);
                    return;
                }
            }
            deletedCallback.deleted(true);
        }
    }

    public static class ImagesFolder {
        private String folderName;
        private ArrayList<ImageFile> imageFiles;
        private long folderId;
        private ImageFile topImage;


        public ImageFile getTopImage() {
            return topImage;
        }

        public long getFolderId() {
            return folderId;
        }
        public void setFolderId(long folderId) {
            this.folderId = folderId;
        }
        public String getFolderName() {
            return folderName;
        }
        public ArrayList<ImageFile> getAllImageFiles() {
            return imageFiles;
        }
        public void setFolderName(String name) {
            this.folderName = name;
        }
        public void setAllImageFiles(ArrayList<ImageFile> imageFiles) {
            this.imageFiles = imageFiles;
        }
    }

    public static ArrayList<ImagesFolder> FindAllFoldersWithImages(@NonNull ContentResolver contentResolver) {

        ALL_FOLDERS.clear();
        boolean is_folder_already_added = false;
        int position = 0;
        int column_index_data, column_bucket_name,column_bucket_id,column_id,column_date_modified,column_display_name,column_size;
        Uri uri;
        uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {MediaStore.MediaColumns.DATA, MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.Images.Media.BUCKET_ID,MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATE_ADDED, MediaStore.Images.Media.SIZE};

        final String orderBy = MediaStore.Images.Media.DATE_TAKEN;
        final Cursor cursor = contentResolver.query(uri, projection, null, null, orderBy + " DESC");
        if (cursor != null) {
            column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            column_bucket_name = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
            column_bucket_id = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID);
            column_id = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            column_date_modified = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED);
            column_display_name = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
            column_size = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(column_id);
                String displayName = cursor.getString(column_display_name);
                Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                long dateModified = TimeUnit.SECONDS.toMillis(cursor.getLong(column_date_modified));
                long size = cursor.getLong(column_size);
                String absolutePathOfImage = cursor.getString(column_index_data);
                long bucketId = cursor.getLong(column_bucket_id);
                String bucketName = cursor.getString(column_bucket_name);


//                Log.i("Path", absolutePathOfImage);
//                Log.i("Folder", bucketName);
//                Log.i("FolderID", String.valueOf(bucketId));

                for (int i = 0; i < ALL_FOLDERS.size(); i++) {
                    String fname = ALL_FOLDERS.get(i).getFolderName();
                    if (fname == null){
                        ALL_FOLDERS.remove(i);
                        i--;
                        continue;
                    }

                    if (fname.equals(bucketName)) {
                        is_folder_already_added = true;
                        position = i;
                        break;
                    } else {
                        is_folder_already_added = false;
                    }
                }

                if (is_folder_already_added) {
                    ALL_FOLDERS.get(position).getAllImageFiles().add(new ImageFile(id, contentUri, displayName, dateModified, size, absolutePathOfImage));
                } else {
                    ArrayList<ImageFile> imageFileList = new ArrayList<>();
                    imageFileList.add(new ImageFile(id, contentUri, displayName, dateModified, size, absolutePathOfImage));

                    ImagesFolder newFolder = new ImagesFolder();
                    newFolder.setFolderName(bucketName);
                    newFolder.setFolderId(bucketId);
                    newFolder.setAllImageFiles(imageFileList);
                    ALL_FOLDERS.add(newFolder);
                }
            }
            cursor.close();
        }
        //find latest image:
        ALL_FOLDERS.forEach(imagesFolder -> {
            imagesFolder.getAllImageFiles().sort(Comparator.comparingLong(value -> -value.getLastModified()));
            imagesFolder.topImage = imagesFolder.getAllImageFiles().get(0);
        });
        ALL_FOLDERS.sort(Comparator.comparing(o -> o.folderName));
        return ALL_FOLDERS;
    }

}
