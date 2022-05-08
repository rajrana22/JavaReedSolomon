arguments=""

while getopts n:k:f: flag
do
    case "${flag}" in
        n) arguments=$arguments${OPTARG};;
        k) arguments=$arguments" "${OPTARG};;
        f) filename=${OPTARG};;
    esac
done

if [ "$arguments" == "" ]
then
    ./gradlew run > $filename
else
    ./gradlew run --args="$arguments" > $filename
fi