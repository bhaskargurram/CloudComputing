inputPath=$(python get_image_from_s3.py $1)
inputSplit=(${inputPath//./ })
outputPath=${inputSplit[0]}-out.${inputSplit[1]}

cd connect_device_package
python mqtt_publish.py $2 $3 $4 &> temp_out.txt
cd ..

python  evaluate.py --checkpoint $5 --in-path $inputPath --out-path $outputPath &> temp_out.txt
python save_image_to_s3.py $outputPath
rm -rf $outputPath $inputPath
