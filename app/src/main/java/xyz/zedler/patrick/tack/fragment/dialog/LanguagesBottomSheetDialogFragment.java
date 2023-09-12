package xyz.zedler.patrick.tack.fragment.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.adapter.LanguageAdapter;
import xyz.zedler.patrick.tack.databinding.FragmentBottomsheetLanguagesBinding;
import xyz.zedler.patrick.tack.model.Language;
import xyz.zedler.patrick.tack.util.LocaleUtil;
import xyz.zedler.patrick.tack.util.UiUtil;

public class LanguagesBottomSheetDialogFragment extends BaseBottomSheetDialogFragment
    implements LanguageAdapter.LanguageAdapterListener {

  private static final String TAG = "LanguagesBottomSheet";

  private FragmentBottomsheetLanguagesBinding binding;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState) {
    binding = FragmentBottomsheetLanguagesBinding.inflate(inflater, container, false);

    MainActivity activity = (MainActivity) requireActivity();

    binding.recyclerLanguages.setLayoutManager(
        new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
    );
    binding.recyclerLanguages.setAdapter(
        new LanguageAdapter(
            LocaleUtil.getLanguages(activity),
            LocaleUtil.getLanguageCode(AppCompatDelegate.getApplicationLocales()),
            this
        )
    );

    return binding.getRoot();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    binding = null;
  }

  @Override
  public void onItemRowClicked(@Nullable Language language) {
    String code = language != null ? language.getCode() : null;
    LocaleListCompat previous = AppCompatDelegate.getApplicationLocales();
    LocaleListCompat selected = LocaleListCompat.forLanguageTags(code);
    if (!previous.equals(selected)) {
      performHapticClick();
      dismiss();
      AppCompatDelegate.setApplicationLocales(selected);
    }
  }

  @Override
  public void applyBottomInset(int bottom) {
    binding.recyclerLanguages.setPadding(
        0, UiUtil.dpToPx(requireContext(), 8),
        0, UiUtil.dpToPx(requireContext(), 8) + bottom
    );
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
