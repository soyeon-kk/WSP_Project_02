from django.urls import path, include
from rest_framework import routers
from .views import (
    post_list, post_detail, post_new, post_edit, post_delete, js_test,
    BlogImage
)

# DRF 라우터 설정
router = routers.DefaultRouter()
router.register(r'Post', BlogImage)

urlpatterns = [
    # HTML 페이지용 URL
    path('', post_list, name='post_list'),
    path('post/<int:pk>/', post_detail, name='post_detail'),
    path('post/new/', post_new, name='post_new'),
    path('post/<int:pk>/edit/', post_edit, name='post_edit'),
    path('post/<int:pk>/delete/', post_delete, name='post_delete'),
    path('js_test/', js_test, name='js_test'),

    # DRF API 주소: /api_root/Post/
    path('api_root/', include(router.urls)),
]
