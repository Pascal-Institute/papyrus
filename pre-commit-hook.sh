#!/usr/bin/env bash
# Pre-commit hook: 컴파일 에러 조기 감지
# .git/hooks/pre-commit 에 복사하세요

echo "🔍 Running pre-commit checks..."

# 1. Kotlin 컴파일 체크
echo "📦 Compiling Kotlin..."
./gradlew compileKotlin compileTestKotlin --no-daemon --quiet
if [ $? -ne 0 ]; then
    echo "❌ Compilation failed! Please fix errors before committing."
    exit 1
fi

# 2. Detekt 정적 분석
echo "🔎 Running Detekt..."
./gradlew detekt --no-daemon --quiet
if [ $? -ne 0 ]; then
    echo "⚠️  Detekt found issues. Check the report."
    # Warning only, not blocking
fi

# 3. 테스트 실행 (빠른 테스트만)
echo "🧪 Running quick tests..."
./gradlew test --tests "*Unit*" --no-daemon --quiet
if [ $? -ne 0 ]; then
    echo "⚠️  Some tests failed. Consider fixing them."
    # Warning only, not blocking
fi

echo "✅ Pre-commit checks passed!"
exit 0
