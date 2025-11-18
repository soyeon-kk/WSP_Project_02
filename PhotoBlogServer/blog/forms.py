# blog/forms.py
from django import forms
from .models import Post

class PostForm(forms.ModelForm):
    class Meta:
        model = Post
        fields = ('author', 'title', 'text', 'image')
        widgets = {
            # 기본 위젯을 ClearableFileInput 대신 FileInput으로 교체
            'image': forms.FileInput(attrs={'id': 'id_image'})
        }
