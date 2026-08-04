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
 * Copyright (c) 2020-2026 by Patrick Zedler
 */

package xyz.zedler.patrick.tack.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ScaleDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.FrameLayout
import androidx.core.graphics.ColorUtils
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.motion.MotionUtils
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.Constants.SONGS_ORDER
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.database.relations.SongWithParts
import xyz.zedler.patrick.tack.databinding.ViewSongPickerBinding
import xyz.zedler.patrick.tack.recyclerview.adapter.SongChipAdapter
import xyz.zedler.patrick.tack.recyclerview.decoration.SongChipItemDecoration
import xyz.zedler.patrick.tack.recyclerview.layoutmanager.WrapperLinearLayoutManager
import xyz.zedler.patrick.tack.util.ViewUtil
import xyz.zedler.patrick.tack.util.getAttrColor
import xyz.zedler.patrick.tack.util.getColorHighlight
import xyz.zedler.patrick.tack.util.getDimension
import xyz.zedler.patrick.tack.util.isLandTablet
import xyz.zedler.patrick.tack.util.isOrientationPortrait
import xyz.zedler.patrick.tack.util.isLayoutRtl
import xyz.zedler.patrick.tack.util.sendSongsWidgetUpdate
import xyz.zedler.patrick.tack.util.dpToPx
import xyz.zedler.patrick.tack.util.setTooltipText
import xyz.zedler.patrick.tack.util.showMenu
import xyz.zedler.patrick.tack.util.startIcon
import xyz.zedler.patrick.tack.util.sortSongsWithParts
import xyz.zedler.patrick.tack.util.OnMenuInflatedListener
import android.content.res.ColorStateList
import androidx.core.view.isInvisible

class SongPickerView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

  private val binding = ViewSongPickerBinding.inflate(
    LayoutInflater.from(context), this
  )
  private val isRtl = context.isLayoutRtl()
  private val heightCollapsed = context.dpToPx(56f)
  private val heightExpanded = context.dpToPx(48 * 3 + 8 * 2f)
  private val heightExpandedMargin = context.dpToPx(32f)

  private val colorBgCollapsed = context.getAttrColor(R.attr.colorSecondaryContainer)
  private val colorBgExpanded = context.getAttrColor(R.attr.colorSurfaceContainer)

  private val chipCloseIconWidth = context.dpToPx(18f)
  private val colorSurfaceContainer = context.getAttrColor(R.attr.colorSurfaceContainer)
  private val colorOnSurface = context.getAttrColor(R.attr.colorOnSurface)
  private val colorOnSurfaceVariant = context.getAttrColor(R.attr.colorOnSurfaceVariant)

  private val viewUtil = ViewUtil()
  private var listener: SongPickerListener? = null
  private var songsWithParts: MutableList<SongWithParts> = mutableListOf()
  private var sortOrder = SONGS_ORDER.NAME_ASC
  private var partIndex = 0
  private var widthMax = 0
  private var widthMin = 0
  private var chipTargetTranslationX = 0
  private var currentSongId = Constants.SONG_ID_DEFAULT

  private lateinit var gradientLeft: ScaleDrawable
  private lateinit var gradientRight: ScaleDrawable

  private var springAnimationExpandSpatial: SpringAnimation? = null
  private var springAnimationExpandEffects: SpringAnimation? = null
  private var springAnimationDeselectSpatial: SpringAnimation? = null
  private var springAnimationDeselectEffects: SpringAnimation? = null
  private var springAnimationSelectSpatial: SpringAnimation? = null
  private var springAnimationSelectEffects: SpringAnimation? = null

  private var expandSpatialFraction = 0f
  private var expandEffectsFraction = 0f
  private var selectSpatialFraction = 0f
  private var selectEffectsFraction = 0f

  var isInitialized = false
    private set
  private var isExpanded = false

  fun setListener(listener: SongPickerListener) {
    this.listener = listener
  }

  fun init(
    currentSongId: String,
    currentPartIndex: Int,
    songs: List<SongWithParts>,
    sortOrder: Int,
    expanded: Boolean
  ) {
    if (isInitialized) return
    isInitialized = true

    this.sortOrder = sortOrder
    this.currentSongId = currentSongId
    this.songsWithParts = songs.toMutableList()

    initPickerSize(expanded, currentSongId)
    initRecycler()
    initChip()
    setCurrentSong(currentSongId, false)
    setPartIndex(currentPartIndex)
  }

  fun setSongs(songs: List<SongWithParts>) {
    this.songsWithParts = songs.toMutableList()
    binding.textSongPickerEmpty.visibility = if (songsWithParts.isEmpty()) VISIBLE else GONE

    sortSongs()

    if (currentSongId == Constants.SONG_ID_DEFAULT) return

    val songName = getSongNameFromId(currentSongId)
    val chipText = binding.textSongPickerChip.text.toString()
    if (songName != null && songName != chipText) {
      binding.textSongPickerChip.text = songName
    }
    setPartIndex(partIndex)
  }

  private fun sortSongs() {
    sortSongsWithParts(songsWithParts, sortOrder)
    val adapter = binding.recyclerSongPicker.adapter as? SongChipAdapter
    if (adapter != null) {
      adapter.submitList(songsWithParts) { maybeCenterSongChips() }
    } else {
      throw IllegalStateException("init() has to be called before any other method")
    }
  }

  fun setPartIndex(partIndex: Int) {
    this.partIndex = partIndex
    val partName = getPartNameFromIndex(partIndex)
    val partLabel = if (partName != null) {
      context.getString(R.string.label_part_current, partIndex + 1, partName)
    } else {
      context.getString(R.string.label_part_unnamed, partIndex + 1)
    }
    binding.buttonSongPickerPart.text = partLabel
    val partCount = getPartCount()
    binding.buttonSongPickerPartPrevious.isEnabled = partCount > 0 && partIndex > 0
    binding.buttonSongPickerPartNext.isEnabled = partCount > 0 && partIndex < partCount - 1
  }

  fun setParentWidth(width: Int) {
    var w = width
    if (context.isOrientationPortrait() || context.isLandTablet()) {
      w = w.coerceAtMost(context.getDimension(R.dimen.max_content_width))
    }
    widthMax = w - context.dpToPx(32f) // 16 + 16 horizontal margin
  }

  private fun initPickerSize(expanded: Boolean, currentSongId: String) {
    binding.buttonSongPickerExpand.setOnClickListener {
      if (isExpanded) return@setOnClickListener
      listener?.onExpandCollapseClicked(true)
      setExpanded(expanded = true, animated = true)
    }
    binding.buttonSongPickerCollapse.setOnClickListener {
      if (!isExpanded) return@setOnClickListener
      listener?.onExpandCollapseClicked(false)
      setExpanded(expanded = false, animated = true)
    }
    binding.buttonSongPickerCollapse.setTooltipText(R.string.action_collapse)
    binding.frameSongPickerTop.setOnClickListener {
      binding.buttonSongPickerCollapse.performClick()
    }

    binding.buttonSongPickerExpand.viewTreeObserver.addOnGlobalLayoutListener(
      object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
          widthMin = binding.buttonSongPickerExpand.width
          setExpanded(expanded, false)
          if (expanded) {
            setCurrentSong(currentSongId, false)
          }
          if (binding.buttonSongPickerExpand.viewTreeObserver.isAlive) {
            binding.buttonSongPickerExpand.viewTreeObserver.removeOnGlobalLayoutListener(this)
          }
        }
      }
    )

    binding.buttonSongPickerOpen.setOnClickListener {
      listener?.onOpenSongsClicked()
    }
    binding.buttonSongPickerOpen.setTooltipText(R.string.action_show_songs_list)

    binding.buttonSongPickerMenu.setOnClickListener {
      listener?.onMenuOrMenuItemClicked()
      val itemClickListener = androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener { item ->
        val id = item.itemId
        if (viewUtil.isClickDisabled(id)) return@OnMenuItemClickListener false
        listener?.onMenuOrMenuItemClicked()

        when (id) {
          R.id.action_sort_name, R.id.action_sort_last_played, R.id.action_sort_most_played -> {
            if (item.isChecked) return@OnMenuItemClickListener false
            sortOrder = when (id) {
              R.id.action_sort_name -> SONGS_ORDER.NAME_ASC
              R.id.action_sort_last_played -> SONGS_ORDER.LAST_PLAYED_ASC
              else -> SONGS_ORDER.MOST_PLAYED_ASC
            }
            item.isChecked = true
            setSongs(songsWithParts)
            sortSongs()
            listener?.onSortOrderChanged(sortOrder)
            if (songsWithParts.isNotEmpty()) {
              sendSongsWidgetUpdate(context)
            }
          }

          R.id.action_backup -> {
            listener?.onBackupClicked()
          }
        }
        true
      }
      val menuInflatedListener = OnMenuInflatedListener { menu: android.view.Menu ->
        val itemId = when (sortOrder) {
          SONGS_ORDER.LAST_PLAYED_ASC -> R.id.action_sort_last_played
          SONGS_ORDER.MOST_PLAYED_ASC -> R.id.action_sort_most_played
          else -> R.id.action_sort_name
        }
        menu.findItem(itemId)?.isChecked = true
      }
      it.showMenu(
        R.menu.menu_song_picker,
        itemClickListener,
        menuInflatedListener
      )
    }
    binding.buttonSongPickerMenu.setTooltipText(R.string.action_more)

    binding.buttonSongPickerAddSong.setOnClickListener {
      listener?.onAddSongClicked()
    }
  }

  private fun initRecycler() {
    val onSongClickListener = object : SongChipAdapter.OnSongClickListener {
      override fun onSongClick(song: SongWithParts) {
        val id = song.song.id
        listener?.onCurrentSongChanged(id)
        setCurrentSong(id, true)
      }

      override fun onSongLongClick(song: SongWithParts) {
        listener?.onSongLongClicked(song.song.id)
      }
    }
    binding.recyclerSongPicker.adapter = SongChipAdapter(
      onSongClickListener, currentSongId == Constants.SONG_ID_DEFAULT
    )
    binding.recyclerSongPicker.layoutManager = WrapperLinearLayoutManager(
      context, LinearLayoutManager.HORIZONTAL, false
    )
    val isPortrait = context.isOrientationPortrait()
    val isLandTablet = context.isLandTablet()
    binding.recyclerSongPicker.isHorizontalFadingEdgeEnabled = !isPortrait && !isLandTablet

    maybeCenterSongChips()
  }

  @SuppressLint("RtlHardcoded")
  private fun initChip() {
    binding.textSongPickerChip.text = getSongNameFromId(currentSongId)
    binding.frameSongPickerChipClose.setOnClickListener {
      listener?.onCurrentSongChanged(Constants.SONG_ID_DEFAULT)
      setCurrentSong(Constants.SONG_ID_DEFAULT, true)
    }
    binding.imageSongPickerChipClose.setOnClickListener {
      binding.frameSongPickerChipClose.callOnClick()
    }
    ViewCompat.setAccessibilityDelegate(
      binding.imageSongPickerChipClose,
      object : AccessibilityDelegateCompat() {
        override fun onInitializeAccessibilityNodeInfo(
          host: View,
          info: AccessibilityNodeInfoCompat
        ) {
          super.onInitializeAccessibilityNodeInfo(host, info)
          info.className = Button::class.java.name
        }
      }
    )
    binding.frameSongPickerChipTouchTarget.setOnClickListener {
      binding.imageSongPickerChipIcon.startIcon()
      listener?.onCurrentSongClicked()
    }
    binding.cardSongPickerChip.setOnClickListener {
      binding.frameSongPickerChipTouchTarget.callOnClick()
    }
    binding.cardSongPickerChip.setOnLongClickListener {
      binding.frameSongPickerChipTouchTarget.callOnClick()
      true
    }
    binding.buttonSongPickerPart.setOnClickListener {
      listener?.onCurrentPartClicked()
    }
    binding.buttonSongPickerPart.setTooltipText(R.string.action_show_parts)
    binding.buttonSongPickerPartPrevious.setOnClickListener {
      listener?.onPreviousPartClicked()
    }
    binding.buttonSongPickerPartPrevious.setTooltipText(R.string.action_prev_part)
    binding.buttonSongPickerPartNext.setOnClickListener {
      listener?.onNextPartClicked()
    }
    binding.buttonSongPickerPartNext.setTooltipText(R.string.action_next_part)

    val gLeft = GradientDrawable(
      GradientDrawable.Orientation.LEFT_RIGHT,
      intArrayOf(
        Color.TRANSPARENT,
        colorSurfaceContainer,
        colorSurfaceContainer,
        colorSurfaceContainer
      )
    )
    gradientLeft = ScaleDrawable(gLeft, Gravity.RIGHT, 1f, 0f)

    val gRight = GradientDrawable(
      GradientDrawable.Orientation.RIGHT_LEFT,
      intArrayOf(
        Color.TRANSPARENT,
        colorSurfaceContainer,
        colorSurfaceContainer,
        colorSurfaceContainer
      )
    )
    gradientRight = ScaleDrawable(gRight, Gravity.LEFT, 1f, 0f)

    binding.viewSongPickerGradientStart.background = if (isRtl) gradientRight else gradientLeft
    binding.viewSongPickerGradientEnd.background = if (isRtl) gradientLeft else gradientRight
  }

  @SuppressLint("PrivateResource")
  fun setExpanded(expanded: Boolean, animated: Boolean) {
    this.isExpanded = expanded
    listener?.onExpandChanged(expanded)

    springAnimationExpandSpatial?.cancel()
    if (animated) {
      if (springAnimationExpandSpatial == null) {
        springAnimationExpandSpatial = SpringAnimation(
          this, EXPAND_SPATIAL_FRACTION
        ).apply {
          spring = MotionUtils.resolveThemeSpringForce(
            context,
            R.attr.motionSpringDefaultSpatial,
            R.style.Motion_Material3_Spring_Standard_Default_Spatial
          ).apply {
            if (TEST_ANIMATIONS) {
              stiffness = 30f
              dampingRatio = 0.9f
            }
          }
          minimumVisibleChange = 0.01f
          addEndListener { _, canceled, _, _ ->
            if (!canceled) setExpandAnimationEndState()
          }
        }
      }
      if (springAnimationExpandEffects == null) {
        springAnimationExpandEffects = SpringAnimation(
          this, EXPAND_EFFECTS_FRACTION
        ).apply {
          spring = MotionUtils.resolveThemeSpringForce(
            context,
            R.attr.motionSpringDefaultEffects,
            R.style.Motion_Material3_Spring_Standard_Default_Effects
          ).apply {
            if (TEST_ANIMATIONS) {
              stiffness = 30f
              dampingRatio = 0.9f
            }
          }
          minimumVisibleChange = 0.01f
        }
      }
      setExpandAnimationStartState()
      springAnimationExpandSpatial?.animateToFinalPosition(if (expanded) 1f else 0f)
      springAnimationExpandEffects?.animateToFinalPosition(if (expanded) 1f else 0f)
    } else {
      setExpandAnimationStartState()
      setExpandSpatialFraction(if (expanded) 1f else 0f)
      setExpandEffectsFraction(if (expanded) 1f else 0f)
      setExpandAnimationEndState()
    }
  }

  private fun setExpandAnimationStartState() {
    binding.buttonSongPickerExpand.isClickable = !isExpanded
    binding.buttonSongPickerCollapse.isClickable = isExpanded
    binding.frameSongPickerTop.isClickable = isExpanded
    binding.buttonSongPickerOpen.isClickable = isExpanded
    binding.buttonSongPickerMenu.isClickable = isExpanded
    binding.buttonSongPickerAddSong.isClickable = isExpanded

    binding.buttonSongPickerExpand.visibility = VISIBLE
    binding.buttonSongPickerCollapse.visibility = VISIBLE
    binding.buttonGroupSongPickerTools.visibility = VISIBLE
    binding.recyclerSongPicker.visibility = VISIBLE
    setRecyclerClicksEnabled(false)
    binding.buttonSongPickerAddSong.visibility = VISIBLE
  }

  private fun setExpandAnimationEndState() {
    binding.buttonSongPickerExpand.visibility = if (isExpanded) INVISIBLE else VISIBLE
    binding.buttonSongPickerCollapse.visibility = if (isExpanded) VISIBLE else GONE
    binding.buttonGroupSongPickerTools.visibility = if (isExpanded) VISIBLE else GONE
    binding.recyclerSongPicker.visibility = if (isExpanded) VISIBLE else INVISIBLE
    setRecyclerClicksEnabled(isExpanded)
    if (isExpanded) {
      maybeCenterSongChips()
    }
    binding.buttonSongPickerAddSong.visibility = if (isExpanded) VISIBLE else GONE
  }

  private fun setExpandSpatialFraction(fraction: Float) {
    expandSpatialFraction = fraction
    val lp = layoutParams ?: return
    lp.width = (widthMin + (widthMax - widthMin) * fraction.coerceAtLeast(0f)).toInt()
    lp.height = (heightCollapsed + (heightExpanded - heightCollapsed) * fraction).toInt()
    layoutParams = lp
    listener?.onHeightChanged()
  }

  fun getExpandSpatialFraction(): Float = expandSpatialFraction

  private fun setExpandEffectsFraction(fraction: Float) {
    expandEffectsFraction = fraction
    binding.buttonSongPickerExpand.alpha = 1 - fraction
    binding.cardSongPickerContainer.setCardBackgroundColor(
      ColorUtils.blendARGB(colorBgCollapsed, colorBgExpanded, fraction)
    )
    binding.buttonSongPickerCollapse.alpha = fraction
    binding.buttonGroupSongPickerTools.alpha = fraction
    binding.recyclerSongPicker.alpha = fraction
    binding.buttonSongPickerAddSong.alpha = fraction
  }

  fun getExpandEffectsFraction(): Float = expandEffectsFraction

  fun getHeightExpanded(): Int = heightExpanded + heightExpandedMargin

  private fun setCurrentSong(currentSongId: String, animated: Boolean) {
    val isDefaultSong = currentSongId == Constants.SONG_ID_DEFAULT
    val position = getPositionOfSong(if (isDefaultSong) this.currentSongId else currentSongId)
    this.currentSongId = currentSongId

    springAnimationSelectSpatial?.cancel()
    springAnimationSelectEffects?.cancel()
    springAnimationDeselectSpatial?.cancel()
    springAnimationDeselectEffects?.cancel()

    if (animated) {
      if (springAnimationSelectSpatial == null) {
        springAnimationSelectSpatial = SpringAnimation(this, SELECT_SPATIAL_FRACTION).apply {
          spring = SpringForce().apply {
            stiffness = if (TEST_ANIMATIONS) 20f else 300f
            dampingRatio = if (TEST_ANIMATIONS) 0.3f else 0.6f
          }
          minimumVisibleChange = 0.01f
          addEndListener { _, canceled, _, _ ->
            if (!canceled) setSelectAnimationEndState()
          }
        }
      }
      if (springAnimationSelectEffects == null) {
        springAnimationSelectEffects = SpringAnimation(this, SELECT_EFFECTS_FRACTION).apply {
          spring = SpringForce().apply {
            stiffness = if (TEST_ANIMATIONS) 40f else 300f
            dampingRatio = 1f
          }
          minimumVisibleChange = 0.01f
        }
      }
      if (springAnimationDeselectSpatial == null) {
        springAnimationDeselectSpatial = SpringAnimation(
          this, DESELECT_SPATIAL_FRACTION
        ).apply {
          spring = MotionUtils.resolveThemeSpringForce(
            context,
            R.attr.motionSpringSlowSpatial,
            R.style.Motion_Material3_Spring_Standard_Slow_Spatial
          ).apply {
            if (TEST_ANIMATIONS) {
              stiffness = 30f
              dampingRatio = 0.9f
            }
          }
          minimumVisibleChange = 0.01f
          addEndListener { _, canceled, _, _ ->
            if (!canceled) setSelectAnimationEndState()
          }
        }
      }
      if (springAnimationDeselectEffects == null) {
        springAnimationDeselectEffects = SpringAnimation(
          this, DESELECT_EFFECTS_FRACTION
        ).apply {
          spring = MotionUtils.resolveThemeSpringForce(
            context,
            R.attr.motionSpringSlowEffects,
            R.style.Motion_Material3_Spring_Standard_Slow_Effects
          ).apply {
            if (TEST_ANIMATIONS) {
              stiffness = 40f
              dampingRatio = 1f
            }
          }
          minimumVisibleChange = 0.01f
        }
      }
      setSelectAnimationStartState()

      val layoutManager = binding.recyclerSongPicker.layoutManager
      if (position >= 0 && layoutManager != null) {
        if (isDefaultSong) {
          layoutManager.scrollToPosition(position)
          maybeCenterSongChips()
        }
        binding.recyclerSongPicker.post {
          val targetChip = layoutManager.findViewByPosition(position)
          if (targetChip != null) {
            var startLeft = binding.frameSongPickerChipTouchTarget.left
            if (isDefaultSong) startLeft += (chipCloseIconWidth / 2)
            chipTargetTranslationX = targetChip.left - startLeft
          }
          if (isDefaultSong) {
            springAnimationDeselectSpatial?.animateToFinalPosition(0f)
            springAnimationDeselectEffects?.animateToFinalPosition(0f)
          } else {
            springAnimationSelectSpatial?.animateToFinalPosition(1f)
            springAnimationSelectEffects?.animateToFinalPosition(1f)
          }
        }
      }
    } else {
      setSelectAnimationStartState()
      setSpatialSelectFraction(if (isDefaultSong) 0f else 1f)
      setEffectsSelectFraction(if (isDefaultSong) 0f else 1f)
      setSelectAnimationEndState()
    }
  }

  private fun setSelectAnimationStartState() {
    val isDefaultSong = currentSongId == Constants.SONG_ID_DEFAULT

    binding.buttonSongPickerCollapse.isEnabled = isDefaultSong
    binding.frameSongPickerTop.isClickable = isDefaultSong

    binding.recyclerSongPicker.visibility = VISIBLE
    setRecyclerClicksEnabled(isDefaultSong)
    if (isDefaultSong) {
      binding.recyclerSongPicker.alpha = 0f
    } else {
      binding.textSongPickerChip.text = getSongNameFromId(currentSongId)
      binding.constraintSongPickerChipContainer.translationX = 0f

      val closeIconParams = binding.imageSongPickerChipClose.layoutParams
      closeIconParams.width = 0
      binding.imageSongPickerChipClose.layoutParams = closeIconParams
    }
    binding.frameSongPickerChipClose.isClickable = !isDefaultSong
    binding.frameSongPickerChipTouchTarget.isClickable = !isDefaultSong
    binding.cardSongPickerChip.isClickable = !isDefaultSong
    binding.cardSongPickerChip.isEnabled = !isDefaultSong
    binding.buttonSongPickerPart.isClickable = !isDefaultSong
    binding.buttonSongPickerPartPrevious.isClickable = !isDefaultSong
    binding.buttonSongPickerPartNext.isClickable = !isDefaultSong

    binding.buttonSongPickerAddSong.visibility = VISIBLE
    binding.buttonSongPickerAddSong.isClickable = isDefaultSong
  }

  private fun setSelectAnimationEndState() {
    val isDefaultSong = currentSongId == Constants.SONG_ID_DEFAULT
    binding.recyclerSongPicker.visibility = if (isDefaultSong) VISIBLE else INVISIBLE
    binding.constraintSongPickerChipContainer.visibility = if (isDefaultSong) INVISIBLE else VISIBLE
    binding.buttonSongPickerAddSong.visibility = if (isDefaultSong) VISIBLE else GONE
  }

  private fun setSpatialSelectFraction(fraction: Float) {
    selectSpatialFraction = fraction
    binding.buttonSongPickerCollapse.rotation = 90 * fraction
    binding.constraintSongPickerChipContainer.translationX = (1 - fraction) * chipTargetTranslationX
    if (binding.constraintSongPickerChipContainer.isInvisible) {
      binding.constraintSongPickerChipContainer.visibility = VISIBLE
    }

    val closeIconParams = binding.imageSongPickerChipClose.layoutParams
    closeIconParams.width = (chipCloseIconWidth * fraction).toInt()
    binding.imageSongPickerChipClose.layoutParams = closeIconParams
  }

  private fun getSpatialSelectFraction(): Float = selectSpatialFraction

  private fun setEffectsSelectFraction(fraction: Float) {
    selectEffectsFraction = fraction
    binding.imageSongPickerChipClose.alpha = fraction

    gradientLeft.level = (10000 * fraction).toInt()
    gradientRight.level = (10000 * fraction).toInt()

    binding.buttonSongPickerCollapse.iconTint = ColorStateList.valueOf(
      ColorUtils.blendARGB(colorOnSurfaceVariant, colorOnSurface, fraction)
    )
    binding.buttonSongPickerCollapse.alpha = 1 + (0.38f - 1) * fraction
    binding.recyclerSongPicker.alpha = 1 - fraction
    binding.textSongPickerEmpty.alpha = 1 - fraction
    binding.buttonSongPickerAddSong.alpha = 1 - fraction

    binding.buttonSongPickerPart.alpha = fraction
    binding.buttonSongPickerPartPrevious.alpha = fraction
    binding.buttonSongPickerPartNext.alpha = fraction
  }

  private fun getEffectsSelectFraction(): Float = selectEffectsFraction

  private fun getPositionOfSong(songNameId: String): Int {
    return songsWithParts.indexOfFirst { it.song.id == songNameId }
  }

  private fun maybeCenterSongChips() {
    val outerPadding = context.dpToPx(16f)
    val innerPadding = context.dpToPx(4f)
    if (binding.recyclerSongPicker.itemDecorationCount == 0) {
      binding.recyclerSongPicker.addItemDecoration(
        SongChipItemDecoration(outerPadding, innerPadding, isRtl)
      )
    }
    binding.recyclerSongPicker.viewTreeObserver.addOnGlobalLayoutListener(
      object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
          binding.recyclerSongPicker.invalidateItemDecorations()
          val itemCount = songsWithParts.size
          if (itemCount > 0) {
            var totalWidth = 0
            for (i in 0 until itemCount) {
              val child = binding.recyclerSongPicker.getChildAt(i)
              if (child != null) totalWidth += child.width
            }
            totalWidth += innerPadding * 2 * (itemCount - 1)
            totalWidth += outerPadding * 2
            val shouldCenter = totalWidth < widthMax
            if (shouldCenter) {
              val padding = (widthMax - totalWidth) / 2
              binding.recyclerSongPicker.setPadding(
                if (isRtl) 0 else padding, 0,
                if (isRtl) padding else 0, 0
              )
              binding.recyclerSongPicker.overScrollMode = View.OVER_SCROLL_NEVER
            } else {
              binding.recyclerSongPicker.setPadding(0, 0, 0, 0)
              binding.recyclerSongPicker.overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            }
          }
          binding.recyclerSongPicker.viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
      }
    )
  }

  private fun getSongNameFromId(songId: String): String? {
    return songsWithParts.find { it.song.id == songId }?.song?.name
  }

  private fun getPartCount(): Int {
    return songsWithParts.find { it.song.id == currentSongId }?.parts?.size ?: 0
  }

  private fun getPartNameFromIndex(partIndex: Int): String? {
    return songsWithParts.find { it.song.id == currentSongId }?.parts
      ?.find { it.partIndex == partIndex }?.name
  }

  private fun setRecyclerClicksEnabled(enabled: Boolean) {
    (binding.recyclerSongPicker.adapter as? SongChipAdapter)?.setClickable(enabled)
  }

  interface SongPickerListener {
    fun onCurrentSongChanged(currentSongId: String)
    fun onCurrentSongClicked()
    fun onCurrentPartClicked()
    fun onPreviousPartClicked()
    fun onNextPartClicked()
    fun onSongLongClicked(songId: String)
    fun onExpandCollapseClicked(expand: Boolean)
    fun onOpenSongsClicked()
    fun onMenuOrMenuItemClicked()
    fun onBackupClicked()
    fun onSortOrderChanged(sortOrder: Int)
    fun onAddSongClicked()
    fun onHeightChanged()
    fun onExpandChanged(expanded: Boolean)
  }

  companion object {
    private const val TEST_ANIMATIONS = false

    private val EXPAND_SPATIAL_FRACTION =
      object : FloatPropertyCompat<SongPickerView>("expandSpatialFraction") {
        override fun getValue(delegate: SongPickerView): Float = delegate.getExpandSpatialFraction()
        override fun setValue(delegate: SongPickerView, value: Float) =
          delegate.setExpandSpatialFraction(value)
      }
    private val EXPAND_EFFECTS_FRACTION =
      object : FloatPropertyCompat<SongPickerView>("expandEffectsFraction") {
        override fun getValue(delegate: SongPickerView): Float = delegate.getExpandEffectsFraction()
        override fun setValue(delegate: SongPickerView, value: Float) =
          delegate.setExpandEffectsFraction(value)
      }
    private val SELECT_SPATIAL_FRACTION =
      object : FloatPropertyCompat<SongPickerView>("selectSpatialFraction") {
        override fun getValue(delegate: SongPickerView): Float = delegate.getSpatialSelectFraction()
        override fun setValue(delegate: SongPickerView, value: Float) =
          delegate.setSpatialSelectFraction(value)
      }
    private val SELECT_EFFECTS_FRACTION =
      object : FloatPropertyCompat<SongPickerView>("selectEffectsFraction") {
        override fun getValue(delegate: SongPickerView): Float = delegate.getEffectsSelectFraction()
        override fun setValue(delegate: SongPickerView, value: Float) =
          delegate.setEffectsSelectFraction(value)
      }
    private val DESELECT_SPATIAL_FRACTION =
      object : FloatPropertyCompat<SongPickerView>("deselectSpatialFraction") {
        override fun getValue(delegate: SongPickerView): Float = delegate.getSpatialSelectFraction()
        override fun setValue(delegate: SongPickerView, value: Float) =
          delegate.setSpatialSelectFraction(value)
      }
    private val DESELECT_EFFECTS_FRACTION =
      object : FloatPropertyCompat<SongPickerView>("deselectEffectsFraction") {
        override fun getValue(delegate: SongPickerView): Float = delegate.getEffectsSelectFraction()
        override fun setValue(delegate: SongPickerView, value: Float) =
          delegate.setEffectsSelectFraction(value)
      }
  }
}
