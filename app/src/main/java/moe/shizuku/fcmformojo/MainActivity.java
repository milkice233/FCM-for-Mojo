package moe.shizuku.fcmformojo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import moe.shizuku.fcmformojo.compat.ShizukuCompat;
import moe.shizuku.fcmformojo.settings.MainSettingsFragment;
import moe.shizuku.fcmformojo.utils.ClipboardUtils;

public class MainActivity extends BaseActivity {

    private static final int REQUEST_CODE = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new MainSettingsFragment())
                    .commit();
        }

        checkGoogleServiceFramework();
        requestPermission();
    }

    private void checkGoogleServiceFramework() {
        boolean ok = false;
        try {
            ok = getPackageManager().getApplicationInfo("com.google.android.gsf", 0).enabled;
        } catch (PackageManager.NameNotFoundException e) {
        }

        if (!ok) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_no_google_title)
                    .setMessage(R.string.dialog_no_google_message)
                    .setPositiveButton(R.string.dialog_no_google_exit, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    private void requestPermission() {
        try {
            //TODO HELP NEEDED: BETTER WAY TO ACQUIRE PERMISSION & PATH?
            /*
            StorageManager sm = getSystemService(StorageManager.class);
            StorageVolume volume = sm.getPrimaryStorageVolume();
            Intent intent = volume.createAccessIntent(Environment.DIRECTORY_DOWNLOADS);
            startActivityForResult(intent, REQUEST_CODE);
            */
            boolean requestPermission = true;
            for (UriPermission up : getContentResolver().getPersistedUriPermissions()) {
                Log.d("MainActivity", up.toString());
                if (up.isWritePermission()) {
                    requestPermission = false;
                    FFMSettings.putDownloadUri(up.getUri().toString());
                    break;
                }
            }
            if (requestPermission) {
                Toast.makeText(this, R.string.select_download_dir, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(intent, REQUEST_CODE);
            }
        } catch (Exception e) {
            Toast.makeText(this, R.string.cannot_request_permission, Toast.LENGTH_LONG).show();
            Log.wtf("FFM", "can't use Scoped Directory Access", e);

            Crashlytics.logException(e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                getContentResolver().takePersistableUriPermission(data.getData(),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                FFMSettings.putDownloadUri(data.getData().toString());
                Log.d("FFM", data.getData().toString());
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        boolean isGooglePlay = "com.android.vending"
                .equals(getPackageManager().getInstallerPackageName(BuildConfig.APPLICATION_ID));
        menu.findItem(R.id.action_donate).setVisible(!isGooglePlay);

        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                Dialog dialog = new AlertDialog.Builder(this)
                        .setView(R.layout.dialog_about)
                        .show();
                ((TextView) dialog.findViewById(R.id.icon_credits)).setMovementMethod(LinkMovementMethod.getInstance());
                ((TextView) dialog.findViewById(R.id.icon_credits)).setText(Html.fromHtml(getString(R.string.about_icon_credits)));

                break;
            case R.id.action_donate:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_donate_title)
                        .setMessage(R.string.dialog_donate_message)
                        .setPositiveButton(R.string.dialog_donate_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent(Intent.ACTION_VIEW,
                                        Uri.parse(BuildConfig.DONATE_ALIPAY_URL));
                                ShizukuCompat.startActivity(MainActivity.this, intent, "com.eg.android.AlipayGphone");
                            }
                        })
                        .setNegativeButton(R.string.dialog_donate_no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Toast.makeText(MainActivity.this, "QAQ", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNeutralButton(R.string.dialog_donate_copy, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ClipboardUtils.put(MainActivity.this, "rikka@xing.moe");
                            }
                        })
                        .show();

                break;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
