import sys
import boto3
import botocore

from s3_utils import get_img_from_s3_into_local, get_img

s3bucket = 'ccprojectbucket'
s3Key = sys.argv[1]
s3 = boto3.resource('s3')

localFilePath = get_img(s3bucket, s3Key, s3)
print(localFilePath)
