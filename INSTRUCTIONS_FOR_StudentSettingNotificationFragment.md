# Hướng dẫn thêm Back Navigation cho StudentSettingNotificationFragment

## 1. Import NotificationNavigationHelper

Thêm import vào đầu file `StudentSettingNotificationFragment.java`:

```java
import vn.edu.fpt.zentryapp.notification.util.NotificationNavigationHelper;
```

## 2. Override onViewCreated hoặc tìm back button

Trong method `onViewCreated` của `StudentSettingNotificationFragment`, thêm logic cho back button:

```java
@Override
public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    // Tìm back button (thay đổi ID cho phù hợp)
    ImageView btnBack = view.findViewById(R.id.btnBack);

    if (btnBack != null) {
        btnBack.setOnClickListener(v -> {
            // Thử handle back navigation về NotificationFragment
            if (!NotificationNavigationHelper.handleBackNavigation(this)) {
                // Fallback: sử dụng back navigation mặc định
                requireActivity().onBackPressed();
            }
        });
    }
}
```

## 3. Hoặc implementation đơn giản hơn

Nếu bạn muốn đơn giản hơn, thêm trực tiếp vào `StudentSettingNotificationFragment`:

```java
private void handleBackNavigation() {
    Bundle args = getArguments();
    if (args != null && "NotificationFragment".equals(args.getString("source_fragment"))) {
        // Quay về NotificationFragment
        try {
            Class<?> fragmentClass = Class.forName("vn.edu.fpt.zentryapp.notification.ui.NotificationFragment");
            Fragment fragment = (Fragment) fragmentClass.newInstance();

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, fragment)
                    .commit();
        } catch (Exception e) {
            requireActivity().onBackPressed();
        }
    } else {
        // Back navigation thường
        requireActivity().onBackPressed();
    }
}
```

Và gọi method này trong back button click:

```java
btnBack.setOnClickListener(v -> handleBackNavigation());
```

## 4. Test implementation

Để test, thêm vào `StudentSettingNotificationFragment`:

```java
@Override
public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    // Debug: kiểm tra arguments
    Bundle args = getArguments();
    if (args != null) {
        String source = args.getString("source_fragment");
        boolean shouldNavigateBack = args.getBoolean("should_navigate_back_to_notification", false);
        Log.d("Navigation", "Source: " + source + ", ShouldNavigateBack: " + shouldNavigateBack);
    }

    // Setup back button
    ImageView btnBack = view.findViewById(R.id.btnBack);
    if (btnBack != null) {
        btnBack.setOnClickListener(v -> {
            if (!NotificationNavigationHelper.handleBackNavigation(this)) {
                requireActivity().onBackPressed();
            }
        });
    }
}
```

## Cách hoạt động:

1. **NotificationFragment** → **StudentSettingNotificationFragment**:

   - Truyền `source_fragment="NotificationFragment"`
   - Thêm vào back stack với name `"NotificationToSettings"`

2. **StudentSettingNotificationFragment** → **NotificationFragment**:

   - Kiểm tra arguments có `source_fragment="NotificationFragment"`
   - Thử pop back stack với name `"NotificationToSettings"`
   - Nếu fail → tạo mới NotificationFragment và navigate

3. **Chỉ áp dụng cho flow này**: Logic chỉ hoạt động khi có arguments đúng, không ảnh hưởng đến navigation khác.
