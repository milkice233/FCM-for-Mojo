package moe.shizuku.fcmformojo.viewholder;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
//import android.icu.text.DateFormat;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.iid.FirebaseInstanceId;

import java.util.Date;
import java.util.Locale;
import java.text.DateFormat;

import moe.shizuku.fcmformojo.R;
import moe.shizuku.fcmformojo.model.RegistrationId;
import moe.shizuku.fcmformojo.utils.ClipboardUtils;
import moe.shizuku.support.recyclerview.BaseViewHolder;

/**
 * Created by rikka on 2017/8/16.
 */

public class RegistrationIdViewHolder extends BaseViewHolder<RegistrationId> {

    public static final Creator CREATOR = new Creator<RegistrationId>() {

        @Override
        public BaseViewHolder<RegistrationId> createViewHolder(LayoutInflater inflater, ViewGroup parent) {
            return new RegistrationIdViewHolder(inflater.inflate(R.layout.item_registration_id, parent ,false));
        }
    };

    private TextView title;
    private TextView summary;
    private View delete;

    private DateFormat mDateFormat;

    public RegistrationIdViewHolder(View itemView) {
        super(itemView);

        title = itemView.findViewById(android.R.id.title);
        summary = itemView.findViewById(android.R.id.summary);
        delete = itemView.findViewById(android.R.id.button1);

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int index = getAdapterPosition();
                getAdapter().getItems().remove(index);
                getAdapter().notifyItemRemoved(index);
            }
        });

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Context context = view.getContext();
                new AlertDialog.Builder(context)
                        .setMessage(Html.fromHtml(context.getString(R.string.dialog_token_message, FirebaseInstanceId.getInstance().getToken())))
                        .setPositiveButton(android.R.string.copy, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ClipboardUtils.put(context, getData().getId());
                            }
                        })
                        .setNeutralButton(R.string.dialog_token_share, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                context.startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND)
                                                .putExtra(Intent.EXTRA_TEXT, FirebaseInstanceId.getInstance().getToken())
                                                .setType("text/plain")
                                        , context.getString(R.string.dialog_token_share)));
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });

        mDateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
    }

    @Override
    public void onBind() {
        Context context = itemView.getContext();

        if (getData().getId().equals(FirebaseInstanceId.getInstance().getToken())) {
            title.setText(context.getString(R.string.devices_name_this, getData().getName()));
        } else {
            title.setText(context.getString(R.string.devices_name, getData().getName()));
        }
        summary.setText(context.getString(R.string.register_time, mDateFormat.format(new Date(getData().getTime()))));
    }
}
