package miuix.appcompat.app;

import androidx.fragment.app.FragmentActivity;

/** Compile-only MIUI activity API shim. The device runtime provides the actual class. */
public class AppCompatActivity extends FragmentActivity {
    public ActionBar getAppCompatActionBar() { return null; }
}
