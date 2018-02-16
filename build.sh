echo "cleaning previously built files..."
rm -rf "./bin"
mkdir "./bin"

echo ""

# echo "building facebook4j..."
# javac -d "./bin" -g -cp "./src" src/facebook4j/internal/org/json/*.java src/facebook4j/internal/*/*.java src/facebook4j/*/*.java src/facebook4j/*.java

# echo ""

echo "building FBPost..."
javac -d "./bin" -g -cp "./src;./lib/*" src/fbpost/*.java

echo "done!"
