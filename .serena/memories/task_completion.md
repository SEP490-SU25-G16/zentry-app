# Task Completion Guidelines

When completing a task in the ZentryApp project, follow these steps:

## 1. Code Implementation
- Ensure all requirements are fulfilled
- Follow existing code style and patterns
- Add proper documentation (Javadoc comments)
- Include appropriate logging
- Handle errors and edge cases

## 2. Testing
- Test the feature on a physical device or emulator
- Verify functionality in different scenarios
- Test edge cases and error conditions
- Ensure UI responsiveness and correct behavior

## 3. Code Quality
- Run Lint checks: `./gradlew lint`
- Fix any warnings or errors
- Ensure code is well-formatted
- Remove any unused imports or resources

## 4. Performance Considerations
- Ensure operations that might be slow are run on background threads
- Verify UI doesn't freeze during operations
- Check memory usage, especially with bitmaps and ML models
- Ensure resources are properly released

## 5. Prepare for Review
- Organize commits logically
- Create a detailed pull request description
- List the changes made and testing performed
- Note any known issues or limitations

## 6. Version Control
```bash
# Stage changes
git add .

# Commit with descriptive message
git commit -m "Feature: Implemented [feature name]"

# Push to the feature branch
git push origin feature/branch-name
```

## 7. Documentation
- Update any relevant documentation
- Document API changes if applicable
- Add usage examples if needed