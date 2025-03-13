/*
 * This file is part of Tack Android.
 *
 * Tack Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tack Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tack Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2025 by Patrick Zedler
 */

package xyz.zedler.patrick.tack.fragment;

import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.divider.MaterialDivider;
import com.google.android.material.slider.Slider;
import com.google.android.material.slider.Slider.OnChangeListener;
import com.google.android.material.slider.Slider.OnSliderTouchListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import xyz.zedler.patrick.tack.Constants.CONTRAST;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.EXTRA;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.Constants.SONGS_ORDER;
import xyz.zedler.patrick.tack.Constants.SOUND;
import xyz.zedler.patrick.tack.Constants.THEME;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.behavior.ScrollBehavior;
import xyz.zedler.patrick.tack.behavior.SystemBarBehavior;
import xyz.zedler.patrick.tack.database.entity.Part;
import xyz.zedler.patrick.tack.database.entity.Song;
import xyz.zedler.patrick.tack.database.relations.SongWithParts;
import xyz.zedler.patrick.tack.databinding.FragmentSettingsBinding;
import xyz.zedler.patrick.tack.databinding.FragmentSongsBinding;
import xyz.zedler.patrick.tack.model.MetronomeConfig;
import xyz.zedler.patrick.tack.recyclerview.adapter.SongAdapter;
import xyz.zedler.patrick.tack.recyclerview.adapter.SongAdapter.OnSongClickListener;
import xyz.zedler.patrick.tack.recyclerview.adapter.SongChipAdapter;
import xyz.zedler.patrick.tack.service.MetronomeService;
import xyz.zedler.patrick.tack.util.DialogUtil;
import xyz.zedler.patrick.tack.util.HapticUtil;
import xyz.zedler.patrick.tack.util.LocaleUtil;
import xyz.zedler.patrick.tack.util.MetronomeUtil.MetronomeListener;
import xyz.zedler.patrick.tack.util.MetronomeUtil.MetronomeListenerAdapter;
import xyz.zedler.patrick.tack.util.MetronomeUtil.Tick;
import xyz.zedler.patrick.tack.util.ShortcutUtil;
import xyz.zedler.patrick.tack.util.UiUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;
import xyz.zedler.patrick.tack.view.ThemeSelectionCardView;

public class SongsFragment extends BaseFragment {

  private static final String TAG = SongsFragment.class.getSimpleName();

  private FragmentSongsBinding binding;
  private MainActivity activity;
  private List<SongWithParts> songs;
  private int songsOrder;
  private SongAdapter adapter;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
  ) {
    binding = FragmentSongsBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBarSongs);
    systemBarBehavior.setContainer(binding.constraintSongs);
    systemBarBehavior.setRecycler(binding.recyclerSongs);
    systemBarBehavior.setAdditionalBottomInset(UiUtil.dpToPx(activity, 96));
    systemBarBehavior.setUp();
    SystemBarBehavior.applyBottomInset(binding.fabSongs);

    /*new ScrollBehavior().setUpScroll(
        binding.appBarSongs, null, true
    );*/

    binding.toolbarSongs.setNavigationOnClickListener(getNavigationOnClickListener());
    binding.toolbarSongs.setOnMenuItemClickListener(item -> {
      int id = item.getItemId();
      if (getViewUtil().isClickDisabled(id)) {
        return false;
      }
      performHapticClick();
      if (id == R.id.action_sort_name || id == R.id.action_sort_last_played) {
        if (id == R.id.action_sort_name) {
          songsOrder = SONGS_ORDER.NAME_ASC;
        } else {
          songsOrder = SONGS_ORDER.LAST_PLAYED_ASC;
        }
        item.setChecked(true);
        setSongs(null);
        getSharedPrefs().edit().putInt(PREF.SONGS_ORDER, songsOrder).apply();
      } else if (id == R.id.action_feedback) {
        activity.showFeedbackBottomSheet();
      } else if (id == R.id.action_help) {
        activity.showTextBottomSheet(R.raw.help, R.string.title_help);
      }
      return true;
    });

    songsOrder = getSharedPrefs().getInt(PREF.SONGS_ORDER, DEF.SONGS_ORDER);
    int itemId = songsOrder == SONGS_ORDER.NAME_ASC
        ? R.id.action_sort_name
        : R.id.action_sort_last_played;
    MenuItem itemSort = binding.toolbarSongs.getMenu().findItem(itemId);
    if (itemSort != null) {
      itemSort.setChecked(true);
    }

    adapter = new SongAdapter(new OnSongClickListener() {
      @Override
      public void onSongClick(@NonNull SongWithParts song) {

      }
    });
    binding.recyclerSongs.setAdapter(adapter);
    // Layout manager
    LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
    binding.recyclerSongs.setLayoutManager(layoutManager);
    binding.recyclerSongs.setItemAnimator(new DefaultItemAnimator());

    activity.getSongViewModel().getAllSongsWithParts().observe(
        getViewLifecycleOwner(), this::setSongs
    );

    binding.fabSongs.setOnClickListener(v -> {
      /*String songName = "Test #10";
      Song song = new Song(songName);
      activity.getSongViewModel().insertSong(song);
      Part part = new Part(null, songName, getMetronomeUtil().getConfig());
      activity.getSongViewModel().insertPart(part);*/
    });
  }

  public void setSongs(@Nullable List<SongWithParts> songs) {
    if (songs != null) {
      this.songs = songs;
    }
    if (songsOrder == SONGS_ORDER.NAME_ASC) {
      Collections.sort(
          this.songs,
          (o1, o2) -> o1.getSong().getName().compareTo(
              o2.getSong().getName()
          )
      );
    } else if (songsOrder == SONGS_ORDER.LAST_PLAYED_ASC) {
      Collections.sort(
          this.songs,
          (s1, s2) -> Long.compare(
              s2.getSong().getLastPlayed(), s1.getSong().getLastPlayed()
          )
      );
    }
    adapter.submitList(new ArrayList<>(this.songs));
  }
}