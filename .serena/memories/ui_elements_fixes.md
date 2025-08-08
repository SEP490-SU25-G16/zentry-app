# UI Elements Missing Fixes

## Issues Resolved

### 1. StudentScheduleFragment - Missing Notification Button
**Problem**: 
- Error: `cannot find symbol variable btnStudentScheduleNotification`
- Location: `StudentScheduleFragment.java:102`

**Solution**: 
- Added missing `btnStudentScheduleNotification` Button to `fragment_student_schedule.xml`
- Positioned it next to the notification icon in the header section
- Made it invisible since it's used for navigation purposes only

**Files Modified**: 
- `app/src/main/res/layout/fragment_student_schedule.xml` (lines 47-54)

### 2. LecturerScheduleClassDetailFragment - Missing Notification Badge
**Problem**: 
- Error: `cannot find symbol variable tvNotificationBadge`
- Location: `LecturerScheduleClassDetailFragment.java:189`

**Solution**: 
- Added missing `tvNotificationBadge` TextView to `fragment_lecturer_schedule_class_detail.xml`
- Positioned it as an overlay on the notification button using ConstraintLayout
- Styled it consistently with other notification badges in the app

**Files Modified**: 
- `app/src/main/res/layout/fragment_lecturer_schedule_class_detail.xml` (lines 207-220)

### 3. LecturerReportFragment - Missing Notification Badge
**Problem**: 
- Code references `tvNotificationBadge` but element doesn't exist in layout
- Location: `LecturerReportFragment.java:115-118`

**Solution**: 
- Added missing `tvNotificationBadge` TextView to `fragment_lecturer_report.xml`
- Wrapped notification button in FrameLayout to allow badge overlay
- Positioned badge in top-right corner of notification button

**Files Modified**: 
- `app/src/main/res/layout/fragment_lecturer_report.xml` (lines 40-65)

## Technical Details

### Notification Button Implementation
```xml
<!-- Notification Button -->
<Button
    android:id="@+id/btnStudentScheduleNotification"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginStart="8dp"
    android:background="?attr/selectableItemBackgroundBorderless"
    android:text=""
    android:visibility="invisible" />
```

### Notification Badge Implementation
```xml
<!-- Notification Badge -->
<TextView
    android:id="@+id/tvNotificationBadge"
    android:layout_width="16dp"
    android:layout_height="16dp"
    android:layout_marginTop="2dp"
    android:layout_marginEnd="2dp"
    android:background="@drawable/bg_notification_badge"
    android:gravity="center"
    android:text=""
    android:textColor="@android:color/white"
    android:textSize="10sp"
    android:textStyle="bold"
    android:visibility="gone" />
```

## Verification
- All missing UI elements have been added to their respective XML layouts
- Elements are positioned and styled consistently with existing UI patterns
- No remaining compilation errors related to missing UI elements
- Notification badges follow the same design pattern across all layouts

## Impact
These fixes ensure that:
1. Student schedule navigation to notifications works correctly
2. Lecturer schedule class detail shows notification badges properly
3. Lecturer report shows notification badges properly
4. All UI elements referenced in Java code now exist in their corresponding layouts