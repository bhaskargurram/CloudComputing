import tempfile
import numpy as np
import time
import scipy.misc

def get_img_from_s3_into_local(s3bucket, s3key, s3, localImageFilePath):
    tmp = tempfile.NamedTemporaryFile()
    with open(tmp.name, 'wb') as f:
       s3.Bucket(s3bucket).download_file(s3key, tmp.name)
       tmp.flush()
    img = open(tmp.name).read()
    img = np.clip(img, 0, 255)
    scipy.misc.imsave(localImageFilePath, img)

def save_img_to_s3_from_local(s3bucket, s3key, s3, localImageFilePath):
    img = scipy.misc.imread(localImageFilePath, mode='RGB')
    if not (len(img.shape) == 3 and img.shape[2] == 3):
       img = np.dstack((img,img,img))
    if img_size != False:
       img = scipy.misc.imresize(img, img_size)
    object = s3.Object(s3bucket, s3key)
    object.put(Body=img)

def get_img(s3bucket, s3key, s3):
    tmp = time.strftime("%Y%m%d-%H%M%S") + '.jpg'
    s3.Bucket(s3bucket).download_file(s3key, tmp)
    return tmp

def save_img(s3bucket, s3, localImageFilePath):
    s3_key = 'output/' + localImageFilePath
    object = s3.Object(s3bucket, s3_key)
    with open(localImageFilePath) as handle:
        object.put(Body=handle)
