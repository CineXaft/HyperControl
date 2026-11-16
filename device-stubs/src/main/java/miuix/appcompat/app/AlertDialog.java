package miuix.appcompat.app;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;

/** Compile-only MIUI dialog shim. Device runtime uses MIUI's actual AlertDialog. */
public class AlertDialog extends android.app.Dialog {
    public AlertDialog(Context context) { super(context); }
    public void setMessage(CharSequence message) { }

    public static class Builder {
        public Builder(Context context) { }
        public Builder setTitle(CharSequence title) { return this; }
        public Builder setTitle(int titleRes) { return this; }
        public Builder setMessage(CharSequence message) { return this; }
        public Builder setView(View view) { return this; }
        public Builder setPositiveButton(CharSequence text, DialogInterface.OnClickListener listener) { return this; }
        public Builder setPositiveButton(int text, DialogInterface.OnClickListener listener) { return this; }
        public Builder setNegativeButton(CharSequence text, DialogInterface.OnClickListener listener) { return this; }
        public Builder setNegativeButton(int text, DialogInterface.OnClickListener listener) { return this; }
        public Builder setOnDismissListener(DialogInterface.OnDismissListener listener) { return this; }
        public AlertDialog show() { throw new UnsupportedOperationException("compile-only stub"); }
    }
}
