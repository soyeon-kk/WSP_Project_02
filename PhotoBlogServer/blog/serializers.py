from rest_framework import serializers
from .models import Post
from django.utils import timezone

class PostSerializer(serializers.ModelSerializer):
    author = serializers.CharField(read_only=True)
    created_date = serializers.DateTimeField(read_only=True)
    published_date = serializers.DateTimeField(read_only=True)
    image = serializers.ImageField(required=False, allow_null=True)

    class Meta:
        model = Post
        fields = ['id', 'author', 'title', 'text', 'created_date', 'published_date', 'image']

    def to_representation(self, instance):
        data = super().to_representation(instance)
        url = data.get("image")
        request = self.context.get('request')

        if request and url and url.startswith("/"):
            data["image"] = request.build_absolute_uri(url)

        return data

    def create(self, validated_data):
        request = self.context.get('request')
        username = (
            request.user.username
            if (request and request.user.is_authenticated)
            else "AnonymousUser"
        )

        return Post.objects.create(
            author=username,
            created_date=timezone.now(),
            published_date=timezone.now(),
            **validated_data
        )

    def update(self, instance, validated_data):
        for attr, value in validated_data.items():
            setattr(instance, attr, value)

        instance.published_date = timezone.now()
        instance.save()
        return instance
