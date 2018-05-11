import sys
from s3_utils import save_img
import boto3

s3bucket = 'ccprojectbucket'
localFilePath = sys.argv[1]
s3 = boto3.resource('s3')

save_img(s3bucket, s3, localFilePath)

print(localFilePath)
