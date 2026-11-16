package dev.local.hypercontrol.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Xml;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import com.android.settingslib.miuisettings.preference.Preference;
import com.android.settingslib.miuisettings.preference.PreferenceCategory;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/* JADX INFO: loaded from: classes.dex */
public final class Extras {
    private static String attr(XmlPullParser xmlPullParser, String str) {
        return xmlPullParser.getAttributeValue("http://schemas.android.com/apk/res/android", str);
    }

    public static PreferenceScreen load(Context context, PreferenceManager preferenceManager, String str) throws IOException, PackageManager.NameNotFoundException, XmlPullParserException {
        ZipFile zipFile = new ZipFile(context.getPackageManager().getApplicationInfo("com.cinexaft.hypercontrol", 0).sourceDir);
        ZipEntry entry = zipFile.getEntry("assets/hc_" + str + ".txt");
        if (entry == null) {
            zipFile.close();
            throw new FileNotFoundException(str);
        }
        InputStream inputStream = zipFile.getInputStream(entry);
        try {
            XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
            xmlPullParserNewPullParser.setInput(inputStream, "UTF-8");
            PreferenceScreen preferenceCategoryCreatePreferenceScreen = preferenceManager.createPreferenceScreen(context);
            PreferenceGroup preferenceCategory = preferenceCategoryCreatePreferenceScreen;
            Preference preference = null;
            while (true) {
                int next = xmlPullParserNewPullParser.next();
                if (next == 1) {
                    return preferenceCategoryCreatePreferenceScreen;
                }
                if (next == 2) {
                    String name = xmlPullParserNewPullParser.getName();
                    if (!"PreferenceScreen".equals(name)) {
                        if ("PreferenceCategory".equals(name)) {
                            preferenceCategory = new PreferenceCategory(context);
                            preferenceCategory.setTitle(Strings.t(context, attr(xmlPullParserNewPullParser, "title")));
                            preferenceCategoryCreatePreferenceScreen.addPreference(preferenceCategory);
                            preference = null;
                        } else if (!"intent".equals(name)) {
                            preference = new Preference(context);
                            preference.setTitle(Strings.t(context, attr(xmlPullParserNewPullParser, "title")));
                            String strAttr = attr(xmlPullParserNewPullParser, "fragment");
                            if (strAttr != null) {
                                preference.setFragment(strAttr);
                            }
                            preference.setIconSpaceReserved(false);
                            preferenceCategory.addPreference(preference);
                        } else if (preference != null) {
                            Intent intent = new Intent();
                            String strAttr2 = attr(xmlPullParserNewPullParser, "action");
                            if (strAttr2 != null) {
                                intent.setAction(strAttr2);
                            }
                            String strAttr3 = attr(xmlPullParserNewPullParser, "targetPackage");
                            String strAttr4 = attr(xmlPullParserNewPullParser, "targetClass");
                            if (strAttr3 != null && strAttr4 != null) {
                                intent.setClassName(strAttr3, strAttr4);
                            }
                            preference.setIntent(intent);
                        }
                    }
                }
            }
        } finally {
            inputStream.close();
            zipFile.close();
        }
    }
}
